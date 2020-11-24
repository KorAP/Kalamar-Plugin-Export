<!DOCTYPE html>
<!-- TODO: localization ... -->
<html>
  <head>
    <meta charset="UTF-8">
    <title>Export</title>
    <script src="${assetPath}/js/korap-plugin-latest.js"></script>
    <link href="${assetPath}/css/kalamar-plugin-latest.css" type="text/css" rel="stylesheet" />
    <style>
      progress {
      min-width: 20em;
      width: 20%;
      padding:8px;
      margin-top: 2em;
      };
    </style>   
  </head>
  <body>
    <div class="banner" data-note="Experimental"></div>
    <h1>Export: <code id="export-query"></code></h1>
    <section>
      <form id="export" class="form-table" action="export" method="POST">
        <fieldset>
          <input type="hidden"  id="q" name="q">
          <input type="hidden" id="ql" name="ql">
          <input type="hidden" id="cq" name="cq">

          <fieldset class="form-line">
            <legend>Dateiformat</legend>

            <div style="margin-top: 1em">
              <input type="radio" checked="checked" id="formatrtf" name="format" value="rtf" style="vertical-align: top">
              <label for="formatrtf">
                RTF
                <br /><span style="font-size:8pt">Rich Text Format (Word etc.)</span>
              </label>
            </div>

            <div style="margin-top: 1em">
              <input type="radio" id="formatcsv" name="format" value="csv" style="vertical-align: top">
              <label for="formatcsv">
                CSV
                <br /><span style="font-size:8pt">Comma-separated Values (Excel etc.)</span>
              </label>
            </div>

            <div style="margin-top: 1em">
              <input type="radio" id="formatjson" name="format" value="json" style="vertical-align: top">
              <label for="formatjson">
                JSON
                <br /><span style="font-size:8pt">JavaScript Object Notation (JavaScript etc.)</span>
              </label>
            </div>
          </fieldset>

          <!-- <input type="checkbox" name="islimit" checked="checked" value="limited"> -->
          
          <fieldset class="form-line">
            <legend>Zu exportierender Treffer</legend>
            <input name="hitc" id="hitc" type="number" min="1" max="10000" value="20" />
          </fieldset>

          <progress id="progress" value="0" max="100" style="display: none;">0%</progress>
          
          <input type="submit" value="Exportieren">

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
