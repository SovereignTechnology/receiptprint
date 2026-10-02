(function(){
  var ID='__sov_print_btn';
  function text(){ try { return (document.body && document.body.innerText) || ''; } catch(e){ return ''; } }
  function isReceipt(){
    var t=text();
    if(/Order ID/i.test(t)) return true;
    if(/(^|\n)\s*Total\b/i.test(t) && /Paid/i.test(t)) return true;
    return false;
  }
  function rm(){ var b=document.getElementById(ID); if(b) b.parentNode.removeChild(b); }
  function add(){
    if(document.getElementById(ID)||!document.body) return;
    var b=document.createElement('button');
    b.id=ID;
    b.textContent='\uD83D\uDDA8 Print receipt';
    b.setAttribute('style','position:fixed;right:12px;bottom:96px;z-index:2147483647;padding:14px 20px;font-size:18px;font-weight:600;border-radius:10px;border:0;background:#00875f;color:#fff;box-shadow:0 3px 10px rgba(0,0,0,.45)');
    b.onclick=function(){
      var t=text();
      b.disabled=true; b.textContent='Printing\u2026';
      fetch('http://127.0.0.1:8088/print',{method:'POST',headers:{'Content-Type':'text/plain'},body:t})
        .then(function(r){ return r.json().catch(function(){return {ok:false};}); })
        .then(function(j){ b.textContent=(j&&j.ok)?'\u2713 Printed':'\u2717 Failed'; })
        .catch(function(){ b.textContent='\u2717 No printer'; })
        .then(function(){ setTimeout(function(){ b.textContent='\uD83D\uDDA8 Print receipt'; b.disabled=false; }, 3000); });
    };
    document.body.appendChild(b);
  }
  function run(){ try { if(isReceipt()) add(); else rm(); } catch(e){} }
  run();
  setTimeout(run,1200);
  setTimeout(run,3000);
})();
