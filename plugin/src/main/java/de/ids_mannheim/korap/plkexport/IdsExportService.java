package de.ids_mannheim.korap.plkexport;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/ids-export")
public class IdsExportService {

    @GET
    @Path("exportHtml")
    @Produces(MediaType.TEXT_HTML)
    public String testHtml () {
        return "Export Web Service under construction";
    }


}
