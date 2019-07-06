package com.cursoandroid.restful;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import java.util.HashSet;
import java.util.Set;

import static android.content.ContentValues.TAG;

//El objetivo de esta clase es realizar una representación RESTful del LED
public class LEDResource extends ServerResource {

    /*
        En este código estamos editanto la respuesta de nuestro servidor con getResponse(),
        y le estamos añadiendo diversas cabeceras necesarias para soportar CORS.
     */
    @Options
    public void getCorsSupport() {
        Set<String> head = new HashSet<>();
        head.add("X-Requested-With");
        head.add("Content-Type");
        head.add("Accept");
        getResponse().setAccessControlAllowHeaders(head);

        Set<Method> methods = new HashSet<>();
        methods.add(Method.GET);
        methods.add(Method.POST);
        methods.add(Method.OPTIONS);

        getResponse().setAccessControlAllowMethods(methods);
        getResponse().setAccessControlAllowOrigin("*");
    }

    @Get("json")
    public Representation getState() {
        JSONObject result = new JSONObject();
        try {
            result.put("estado", LEDModel.getState());
        } catch (Exception e) {
            Log.e(TAG, "Error en JSONObject: ", e);
        }
        getResponse().setAccessControlAllowOrigin("*");
        return new StringRepresentation(result.toString(),MediaType.APPLICATION_ALL_JSON);
    }

    @Post("json")
    public Representation postState(Representation entity) {
        JSONObject query = new JSONObject();
        JSONObject fullresult = new JSONObject();
        String result;
        try {
            JsonRepresentation json = new JsonRepresentation(entity);
            query = json.getJsonObject();
            boolean state = (boolean) query.get("estado");
            Log.d(this.getClass().getSimpleName(), "Nuevo estado del LED: " +
                    state);
            if (LEDModel.setState(state)) result = "ok";
            else result = "error";
        } catch (Exception e) {
            Log.e(TAG, "Error: ", e);
            result = "error";
        }
        try {
            fullresult.put("resultado", result);
        } catch (JSONException e) {
            Log.e(TAG, "Error en JSONObject: ", e);
        }

        getResponse().setAccessControlAllowOrigin("*"); //Evita el error descrito en la pág. 46

        return new StringRepresentation(fullresult.toString(),
                MediaType.APPLICATION_ALL_JSON);
    }
}

