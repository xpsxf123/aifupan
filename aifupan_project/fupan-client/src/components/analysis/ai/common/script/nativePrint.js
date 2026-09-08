export function buildNativePrintScript() {
  return `<script>
    (function(){
      var PRINT_EVENT = 'native-print-pdf';
      var DONE_EVENT = 'native-print-pdf-done';
      var ERROR_EVENT = 'native-print-pdf-error';
      try {
        var style = document.createElement('style');
        style.type = 'text/css';
        style.textContent = '@media print { html, body { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }';
        document.head && document.head.appendChild(style);
      } catch(_) {}
      window.addEventListener('message', function(ev){
        var data = ev && ev.data;
        if (!data || data.type !== PRINT_EVENT) return;
        try {
          window.focus && window.focus();
          window.print();
          try { ev.source && ev.source.postMessage({ type: DONE_EVENT }, '*'); } catch(_) {}
        } catch (e) {
          try { ev.source && ev.source.postMessage({ type: ERROR_EVENT, message: e && e.message }, '*'); } catch(_) {}
        }
      });
    })();
  <\/script>`;
}

