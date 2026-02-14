"use strict";

function pluginit(P) {

  // Request query params from the embedding window
  let authToken = null;
  P.requestMsg(
    {
      'action':'get',
      'key':'User'
    },
    function (d) {
      if (d.value && d.value.auth) {
        authToken = d.value.auth;
      };
    }
  );

  P.requestMsg(
    {
      'action':'get',
      'key':'QueryParam'
    },
    function (d) {
      let v = d.value;
      if (v !== undefined && v["q"] && v["q"] !== "") {
        let e = v["q"];
        const eq = document.getElementById("export-query");
        
        document.getElementById("q").value=v["q"];
        if (v["ql"]) {
          e += " " + (eq.dataset.withql || "with")  + " " + v["ql"];
          document.getElementById("ql").value=v["ql"];
        };
        if (v["cq"]) {
          e += " " + (eq.dataset.incq || "in") + " " + v["cq"];
          document.getElementById("cq").value=v["cq"];
        };
        eq.innerText = e;
      }

      else {
        P.log(0, "Query undefined");
      };

      P.resize();
    }
  );

  if (window.dynCall) {
    window.dynCall(P)
  };

  // Toggle advanced options
  const toggle = document.getElementById("options-toggle");
  if (toggle) {
    toggle.onclick = function () {
      const advContent = document.getElementById("advanced-options-content");
      const marker = document.getElementById("options-marker");
      if (advContent) {
        if (advContent.style.display === "none") {
          advContent.style.display = "flex";
          if (marker) marker.innerText = '\u25BC';
        } else {
          advContent.style.display = "none";
          if (marker) marker.innerText = '\u25B6';
        };
      };
    };
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

    let queryDefined = false;
    
    for (let i = 0; i < inputs.length; i++) {
      field = inputs[i];
      if (field.name.length != "" && field.value != "") {
        query.append(field.name, field.value);
        if (field.name === "q")
          queryDefined = true;
      }
    };

    if (!queryDefined) {
      P.log(0, "Query undefined");
      return false;
    }
    
    // Check radio buttons and checkboxes
    inputs = form.querySelectorAll("input[type=radio],input[type=checkbox]");
    for (var i = 0; i < inputs.length; i++) {
      if (inputs[i].checked) {
        query.append(inputs[i].name, inputs[i].value)
      };
    };
    
    if (authToken) {
      query.append("auth", authToken);
    };
    
    reqStream(url.href);
    return false;
  };
};


// Create an eventsource listener for target
function reqStream (target) {

  let relocationURL;

  const sse = new EventSource(target, { withCredentials : true });
  const prog = document.getElementById('progress');

  sse.addEventListener('Progress', function (e) {
    prog.value = e.data;
    prog.textualData = e.data + "%";
  });

  const err = function (e) {
    prog.style.display = "none";
    sse.close();
    window.Plugin.resize();
    let msg = "Connection error";
    if (e.data !== undefined) {
        msg = e.data;
    };
    console.log(msg);
    window.Plugin.log(0, msg);
  };

  sse.addEventListener('Error', err);
  sse.onerror = err;

  sse.addEventListener('Relocate', function (e) {
    if (e.data == undefined || e.data.length == 0) {
      window.Plugin.log(0,"Unable to export file");
      return;
    };

    const data = e.data.split(";");

    if (data.length == 2 && data[0]) {

      relocationURL = new URL(target);
      relocationURL.search = '';
      relocationURL.pathname += '/' + data[0];

      if (data[1] != null && data[1].length > 0) {
        relocationURL.searchParams.append("fname",data[1]);
      };
      
      return;
    };

    window.Plugin.log(0,"Unable to export file");
    
    // Todo:
    //   Start a timer to automatically close
    //   the eventsource

  });

  sse.addEventListener('Process', function (e) {
    prog.style.display = "block";
    if (e.data == "init") {
      window.Plugin.resize();
    }
    else if (e.data == 'done') {
      sse.close();
      prog.value = 100;
      prog.textualData = "100%";

      // Relocate to new target
      if (relocationURL != null)
        location.href = relocationURL.href;
    }
  });
};
