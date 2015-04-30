package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.Session;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by kanemorgan on 01/04/2015.
 */
@Api
public class Approve {

    /**
     * Approves the content of a collection at the endpoint /Approve/[CollectionName].
     *
     * @param request
     * @param response <ul>
     *                 <li>If approval succeeds: {@link HttpStatus#OK_200}</li>
     *                 <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If authentication fails:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>If the collection doesn't exist:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If the collection has incomplete items:  {@link HttpStatus#CONFLICT_409}</li>
     *                 </ul>
     * @return Save successful status of the description.
     * @throws IOException
     */
    @POST
    public boolean approveCollection(HttpServletRequest request, HttpServletResponse response) throws IOException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);

        // Check collection exists
        if (collection == null) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return false;
        }

        // TODO Check user permissions UNAUTHORISED_401
        Session session = Root.zebedee.sessions.get(request);
        if (session == null || !Root.zebedee.permissions.canEdit(session.email)) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return false;
        }

        // TODO Check collection exists BAD_REQUEST_400
        if (collection == null) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return false;
        }

        // Check everything is completed
        if (!collection.inProgressUris().isEmpty() || !collection.completeUris().isEmpty()) {
            response.setStatus(HttpStatus.CONFLICT_409);
            return false;
        }

        response.setStatus(HttpStatus.OK_200);
        collection.description.approvedStatus = true;

        return collection.save();
    }


}
