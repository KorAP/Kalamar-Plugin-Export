<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <title>Export</title>
    <script src="${assetPath}/js/korap-plugin-latest.js"></script>
    <link href="${assetPath}/css/kalamar-plugin-latest.css" type="text/css" rel="stylesheet" />
    <style>

      progress {
        -webkit-appearance: none;
        -moz-appearance: none;
        appearance: none;
        min-width: 20em;
        width: 20%;
        height: 2em;
        margin-top: 2em;
        border-radius: 6px;
        background-color: #d9dadb;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.25) inset;
        background-image: none;
      }

      progress::-webkit-progress-bar {
        border-radius: 6px;
        border: 1px solid #636f07;
        background-color: #d9dadb;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.25) inset;
        background-image: none;
      }
      
      progress::-moz-progress-bar {
        border: 0;
        background-color: #8a9a0a;
        transition: all .3s ease-in-out 0s;
      }

      progress::-webkit-progress-value {
        border: 0;
        background-color: #8a9a0a;
        transition: all .3s ease-in-out 0s;
      }

      progress:indeterminate::-webkit-progress-bar {
        background-color: #f6a800;
      }

      progress:indeterminate::-moz-progress-bar {
        background-color: #f6a800;
      }

      label > .desc {
        font-size: 90%;
        display: block;
      }

      #export input[type=radio] {
        vertical-align: top;
      }

    </style>   
  </head>
  <body>
    <!-- <div class="banner" data-note="${dict.banner}"></div> -->
    <h1>${dict.export}: <code id="export-query" data-withql="${dict.with_ql}" data-incq="${dict.in_cq}"></code></h1>
    <section>
      <form id="export" class="form-table" action="export" method="POST">
        <fieldset>
          <input type="hidden"  id="q" name="q">
          <input type="hidden" id="ql" name="ql">
          <input type="hidden" id="cq" name="cq">

          <fieldset class="form-line">
            <legend>${dict.file_format}</legend>

            <div style="margin-top: 1em">
              <input type="radio"
                     checked="checked"
                     id="formatrtf"
                     name="format"
                     value="rtf">
              <label for="formatrtf">
                RTF <span class="desc">Rich Text Format (Word etc.)</span>
              </label>
            </div>

            <div style="margin-top: 1em">
              <input type="radio"
                     id="formatcsv"
                     name="format"
                     value="csv">
              <label for="formatcsv">
                CSV <span class="desc">Comma-separated Values (Excel etc.)</span>
              </label>
            </div>

            <div style="margin-top: 1em">
              <input type="radio"
                     id="formatjson"
                     name="format"
                     value="json">
              <label for="formatjson">
                JSON <span class="desc">JavaScript Object Notation (JavaScript etc.)</span>
              </label>
            </div>
          </fieldset>

          <fieldset class="form-line">
            <legend>${dict.hitc}</legend>
            <input name="hitc" id="hitc" type="number" min="1" max="10000" value="${defaultHitc}" />
            <p style="font-size: 80%; margin-top: .2em; margin-bottom: 0;">${dict.max_hitc} <tt>${maxHitc}</tt></p>
          </fieldset>

          <progress id="progress" value="0" max="100" style="display: none;">0%</progress>

          <input type="submit" value="${dict.export_button}">
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
