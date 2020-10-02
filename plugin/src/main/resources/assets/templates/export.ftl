<!DOCTYPE html>
<!-- TODO: localization ... -->
<html>
  <head>
    <meta charset="UTF-8">
    <title>Export</title>
    <script src="${assetPath}/js/korap-plugin-latest.js"></script>
    <link href="${assetPath}/css/kalamar-plugin-latest.css" type="text/css" rel="stylesheet" />
  </head>
  <body>
    <div class="banner" data-note="Experimental"></div>
    <h1>Export: <code id="export-query"></code></h1>
    <section>
      <form id="frmid" class="form-table" action="export" method="POST">
        <fieldset>
          <input type="hidden"  id="q" name="q">
          <input type="hidden" id="ql" name="ql">
          <input type="hidden" id="cq" name="cq">
          
          <fieldset class="form-line">
            <legend>Dateiformat</legend>
            <input type="radio" id="formatjson" name="format" value="json">
            <label for="formatjson">JSON</label>
            
            <input type="radio" checked="checked" id="formathtml" name="format" value="rtf">  
            <label for="formatrtf">RTF</label>
          </fieldset>

          <!--
              <div>
                <label>
                  <input type="checkbox" name="islimit" checked="checked" value="limited">
                  Beschränken auf 
                  <input name="hitc" type="number" min="1" max="1000" value="20">
                  Treffer 
                </label>
              </div>
              -->
          <input type="submit" value="Exportieren">

          <p style="font-size: 80%">Das Export-Plugin befindet sich noch in einer experimentellen Phase. Derzeit können nur die ersten 50 Treffer exportiert werden.</p>

        </fieldset>
      </form>
    </section>
    <script src="export.js" defer></script>

    <#if code??>
    <script>//<![CDATA[
      function dynCall (P) {
        P.log(${code}, '${msg}');
      };
    //]]></script>
    </#if>
  </body>
</html>
