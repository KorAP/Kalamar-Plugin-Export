"use strict";

function pluginit(P) {
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
    }
  );
};
