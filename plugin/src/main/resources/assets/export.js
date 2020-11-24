"use strict";

function pluginit(P) {

  // Request query params from the embedding window
  P.requestMsg(
    {
      'action':'get',
      'key':'QueryParam'
    },
    function (d) {
      let v = d.value;
      if (v !== undefined && v["q"]) {
        let e = v["q"];
        document.getElementById("q").value=v["q"];
        if (v["ql"]) {
          e += " with " + v["ql"];
          document.getElementById("ql").value=v["ql"];
        };
        if (v["cq"]) {
          e += " in " + v["cq"];
          document.getElementById("cq").value=v["cq"];
        };
        document.getElementById("export-query").innerText = e;
      };

      P.resize();
    }
  );

  if (window.dynCall) {
    window.dynCall(P)
  };

  // Set plugin object globally
  window.Plugin = P;

  // Convert POST formparams to GET queryparams
  // on submission to be usable in eventsource.
  // This, for the moment, ignores all other form
  // fields and has to be expanded if needed.
  document.getElementById("export").onsubmit = function (e) {
    const form = e.target;
    const url = new URL(form.action !== undefined ? form.action : "", location);
    const query = url.searchParams;

    let field;
    let inputs = form.querySelectorAll("input:not([type=radio]):not([type=checkbox])");
    for (let i = 0; i < inputs.length; i++) {
      field = inputs[i];
      if (field.name.length != "" && field.value != "") {
        query.append(field.name, field.value);
      }
    };

    // Check radio buttons and checkboxes
    inputs = form.querySelectorAll("input[type=radio],input[type=checkbox]");
    for (var i = 0; i < inputs.length; i++) {
      if (inputs[i].checked) {
        query.append(inputs[i].name, inputs[i].value)
      };
    };

    reqStream(url.href);
    return false;
  };
};


// Create an eventsource listener for target
function reqStream (target) {
  const sse = new EventSource(target);
  const prog = document.getElementById('progress');
  sse.addEventListener('Progress', function (e) {
    prog.value = e.data;
    prog.textualData = e.data + "%";
  });
  sse.addEventListener('Error', function (e) {
    prog.style.display = "none";
    sse.close();
    window.plugin.log(0, e.data);
  });
  sse.addEventListener('Relocate', function (e) {
    prog.style.display = "none";
    sse.close();
    console.log(e.data);
  });
  sse.addEventListener('Process', function (e) {
    prog.style.display = "display";
    if (e.data == "init") {
      prog.value = 0;
      prog.textualData = "0%";
      P.resize();
    }
    else if (e.data == 'done') {
      prog.value = 100;
      prog.textualData = "100%";
    }
  });
};
