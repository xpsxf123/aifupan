export function buildChartSlicesScript() {
  return `<script>
    (function(){
      var __sliceTotal = 0;
      var __sliceDone = 0;
      setTimeout(function() {
        clientPrint();
      }, 25000);
      function __markTask(){ __sliceTotal++; }
      function __markDone(){ __sliceDone++; if (__sliceDone >= __sliceTotal) clientPrint(); }
      function pageHeightPx(container){
        var w = (container && container.clientWidth) || 794;
        return Math.floor(w * 297 / 210);
      }
      function insertSlices(img, container){
        var parent = container.parentNode;
        var naturalW = img.naturalWidth || img.width;
        var naturalH = img.naturalHeight || img.height;
        var sliceH = Math.max(100, pageHeightPx(parent) - 24);
        var y = 0;
        while (y < naturalH) {
          var partH = Math.min(sliceH, naturalH - y);
          var cv = document.createElement('canvas');
          cv.width = naturalW;
          cv.height = partH;
          var ctx = cv.getContext('2d');
          if (ctx) {
            ctx.imageSmoothingEnabled = true;
            ctx.imageSmoothingQuality = 'high';
            ctx.drawImage(img, 0, y, naturalW, partH, 0, 0, naturalW, partH);
          }
          var pieceWrap = document.createElement('section');
          pieceWrap.style.pageBreakInside = 'avoid';
          pieceWrap.style.breakInside = 'avoid';
          pieceWrap.style.display = 'block';
          pieceWrap.style.width = '100%';
          if (naturalH - (y + partH) > 0) {
            pieceWrap.style.pageBreakAfter = 'always';
            pieceWrap.style.breakAfter = 'page';
          }
          var piece = document.createElement('img');
          piece.src = cv.toDataURL('image/png');
          piece.style.width = '100%';
          piece.style.height = 'auto';
          piece.style.display = 'block';
          pieceWrap.appendChild(piece);
          parent.insertBefore(pieceWrap, container);
          y += partH;
        }
        parent.removeChild(container);
        __markDone();
      }
      function replaceChartWithSlices(dom){
        try {
          if (!dom || typeof echarts === 'undefined') {
            __markDone();
            return;
          }
          var chart = echarts.getInstanceByDom(dom);
          if (!chart) {
            __markDone();
            return;
          }
          var ratio = Math.max(2, (window.devicePixelRatio || 1) * 2);
          var base64 = chart.getDataURL({ type: 'png', pixelRatio: ratio, backgroundColor: '#ffffff' });
          var img = new Image();
          img.onload = function(){ insertSlices(img, dom); };
          img.onerror = function(){ __markDone(); };
          img.src = base64;
        } catch(e) {
          __markDone();
        }
      }
      function clientPrint(){
        setTimeout(() => {
          if (window?.client && window?.client?.print) {
            window?.client?.print();
          }
        }, 1000);
      }
      function replaceCanvasToSlices(c){
        try{
          var cv = c;
          var img = new Image();
          img.onload = function(){ 
            insertSlices(img, cv); 
          };
          img.onerror = function(){
            __markDone();
          };
          img.src = cv.toDataURL('image/png');
        }catch(e){
          __markDone();
        }
      }
      function findChartContainerFromCanvas(c){
        var node = c;
        while(node){
          if (node.getAttribute && (node.getAttribute('data-ec') || node.getAttribute('_echarts_instance_'))) {
            return node;
          }
          node = node.parentElement;
        }
        return null;
      }
      function bindFinished(chart, dom){
        if (!chart) return;
        var isHandled = false;
        var timer = null;
        function onDone() {
          if (isHandled) return;
          isHandled = true;
          if (timer) clearTimeout(timer);
          requestAnimationFrame(function(){ replaceChartWithSlices(dom); });
        }
        chart.off && chart.off('finished');
        chart.on('finished', onDone);
        // 如果图表在3秒内没有finished（可能已经finished了或者卡住了），强制执行
        timer = setTimeout(onDone, 3000);
      }
      function init(){
        var canvases = document.querySelectorAll('canvas');
        var handled = new Set();
        for (var i = 0; i < canvases.length; i++) {
          var c = canvases[i];
          var container = findChartContainerFromCanvas(c) || c.parentElement || c;
          if (!container || handled.has(container)) continue;
          var chart = null;
          if (typeof echarts !== 'undefined') {
            var ecId = (container.getAttribute && (container.getAttribute('data-ec') || container.getAttribute('_echarts_instance_'))) || null;
            chart = (ecId && echarts.getInstanceById) ? echarts.getInstanceById(ecId) : echarts.getInstanceByDom(container);
            if (chart) {
              handled.add(container);
              __markTask();
              bindFinished(chart, container);
              continue;
            }
          }
          __markTask();
          replaceCanvasToSlices(c);
        }
        if (__sliceTotal === 0) clientPrint();
      }
      function hasCharts(){
        try{
          if (typeof echarts === 'undefined') return false;
          var ecNodes = document.querySelectorAll('[data-ec],[_echarts_instance_]');
          return ecNodes && ecNodes.length > 0;
        }catch(_){ return false; }
      }
      if (!hasCharts()) {
        clientPrint();
      } else {
        if (document.readyState === 'loading') {
          document.addEventListener('DOMContentLoaded', init);
        } else {
          init();
        }
      }
    })();
  <\/script>`;
}

