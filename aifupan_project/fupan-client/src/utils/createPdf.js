/**
 * 将长图片智能分页添加到PDF中，避免内容切割
 * @param {jsPDF} pdf - jsPDF实例
 * @param {number} margin - 页边距(mm)
 */
async function addLongImageWithSmartSplit(pdf, canvasDom, opt = {}) {
  const { w, h, image: imgElement, margin = 0, isBetterSplit = true, checkHeight = 60 } = opt;
  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const imgWidth = pageWidth; // 减去边距
  let scale = 1;
  let currentY = 0;

  if (isBetterSplit) {
    let imgsList = [];
    let { width: cW, height: cH } = canvasDom;  
    let imgHeight = (cH * imgWidth) / cW;
    // 处理比例问题。切图太多会导致内存溢出，缩小图片比例则可以减少内存。
    // 计算图片比例，还有转换后的图片高度
    let imgScale = cH / imgHeight;
    let scaledHeight = imgHeight;
    // 创建canvas分析图片内容
    let canvas = document.createElement('canvas');
    canvas.width = imgWidth;
    canvas.height = imgHeight;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(canvasDom, 0, 0, cW, cH, 0, 0, imgWidth, imgHeight);
    // 分析图片内容密度
    const contentMap =await analyzeImageContent(ctx, imgWidth, imgHeight);
    let num = 0;
    while (currentY < parseInt(scaledHeight) && num < 50) {
      let sectionHeight = pageHeight;
      // 检测分页位置是否切割内容
      // 计算当前的y坐标
      const checkStartY = parseInt(currentY);
      const checkEndY = parseInt(currentY + sectionHeight);
      const checkH = checkHeight || 10; // 检查区域高度
      // 查找最佳分页点
      

      let [betterSplit, endSplit] = findOptimalSplitPoint(
        contentMap,
        checkStartY,
        checkEndY,
        checkH
      );
      // 添加图片到PDF
      // 计算裁剪区域，裁剪图片内容
      const clipHeight = (endSplit - betterSplit) * imgScale;
      const startHeight = betterSplit * imgScale;
      let clipCanvas = document.createElement('canvas');
      clipCanvas.width = cW;
      clipCanvas.height = clipHeight;
      const clipCtx = clipCanvas.getContext('2d');
      // 从原canvas裁剪指定区域
      clipCtx.drawImage(
        canvasDom,
        0, startHeight, // 源图像的裁剪起点
        cW, clipHeight, // 源图像的裁剪宽高
        0, 0, // 目标canvas的起点
        cW, clipHeight // 目标canvas的宽高
      );
      let imgUrl = clipCanvas?.toDataURL('image/png');
      pdf.addImage(imgUrl, 'PNG', 0, 0, imgWidth, (endSplit - betterSplit), null, 'FAST');
      imgsList.push(imgUrl);
      // 添加分页提示(可选)
      if (currentY + sectionHeight < scaledHeight) {
        pdf.addPage();
      }
      currentY = endSplit;
      num++;
    }
    return imgsList;
  } else {
    const imgHeight = (h * imgWidth) / w;
    let scaledHeight = imgHeight;
    while (scaledHeight >= 0) {
      currentY = scaledHeight - imgHeight;
      pdf.addImage(imgElement, 'PNG', 0, currentY, imgWidth, imgHeight, null, 'FAST');
      scaledHeight -= pageHeight;
      if (scaledHeight >= 0) {
        pdf.addPage();
      }
    }
    return [imgElement];
  }

}

/**
 * 分析图片内容密度
 */
async function analyzeImageContent(ctx, width, height) {
  const blockSize = 1; // 分析块大小(px)
  const contentMap = [];
  // 确保高度有效
  if (!height || height <= 0) {
    return contentMap;
  }
  for (let y = 0; y < height; y += blockSize) {
    const blockHeight = Math.min(blockSize, height - y);
    // 只处理有效的块高度
    if (blockHeight >= 1) {
      try {
        const imageData = ctx.getImageData(0, y, width, blockHeight);
        if (imageData && imageData.data) {
          contentMap[y] = calculateContentDensity(imageData.data);
        }
      } catch (error) {
        console.warn('Error getting image data at y:', y, error);
        contentMap[y] = 0;
      }
    }
  }
  return contentMap;
}

/**
 * 计算内容密度(0-1)
 */
function calculateContentDensity(imageData, threshold = 30) {
  let contentPixels = 0;
  const totalPixels = imageData.length / 4;
  if (!totalPixels) {
    return 0;
  }

  const alphaThreshold = 8;
  const quantizeShift = 4;
  const distanceThreshold = 45;
  const sampleStep = 4;

  let bgKey = 0;
  let bgCount = 0;
  const hist = new Map();

  for (let i = 0; i < imageData.length; i += 4 * sampleStep) {
    const a = imageData[i + 3];
    if (a <= alphaThreshold) continue;
    const r = imageData[i];
    const g = imageData[i + 1];
    const b = imageData[i + 2];
    const key = ((r >> quantizeShift) << 8) | ((g >> quantizeShift) << 4) | (b >> quantizeShift);
    const next = (hist.get(key) || 0) + 1;
    hist.set(key, next);
    if (next > bgCount) {
      bgCount = next;
      bgKey = key;
    }
  }

  const bgR = ((bgKey >> 8) & 0xF) * 16 + 8;
  const bgG = ((bgKey >> 4) & 0xF) * 16 + 8;
  const bgB = (bgKey & 0xF) * 16 + 8;

  for (let i = 0; i < imageData.length; i += 4) {
    const a = imageData[i + 3];
    if (a <= alphaThreshold) continue;

    const r = imageData[i];
    const g = imageData[i + 1];
    const b = imageData[i + 2];
    const brightness = (r * 299 + g * 587 + b * 114) / 1000;
    const dist = Math.abs(r - bgR) + Math.abs(g - bgG) + Math.abs(b - bgB);

    if (dist <= distanceThreshold) {
      continue;
    }

    if (brightness > 235 && dist <= distanceThreshold * 2) {
      continue;
    }

    contentPixels++;
  }

  return contentPixels / totalPixels;
}

/**
 * 查找最佳分页点
 * checkHeight 检查范围。
 */
function findOptimalSplitPoint(contentMap, checkStartY, checkEndY, checkHeight) {
  // 开始分割点
  let startSplit = checkStartY;
  // 结束分割点
  let endSplit = checkEndY;
  // 结束分割点查询范围
  const endSplitStart = checkEndY;
  const endSplitEnd = checkEndY - checkHeight;
  // 结束分割点最小密度
  let endMinDensity = 1;
  // 结束分割点查询
  for (let y = endSplitStart; y > endSplitEnd; y--) {
    if (contentMap[y] !== undefined && contentMap[y] < endMinDensity) {
      endMinDensity = contentMap[y];
      endSplit = y;
      if (endMinDensity === 0) {
        break;
      }
    }
  }
  return [startSplit, endSplit];
}

/**
 * 添加分页提示
 */
function addPageContinuationHint(pdf, margin, pageWidth) {
  // pdf.setDrawColor(200, 200, 200);
  // pdf.setLineWidth(0.2);
  // pdf.line(margin, margin - 2, pageWidth - margin, margin - 2);

  // pdf.setTextColor(150);
  // pdf.setFontSize(8);
  pdf.text(
    '继续下一页...',
    pageWidth - margin - 20,
    margin - 5
  );
}

export default addLongImageWithSmartSplit;
