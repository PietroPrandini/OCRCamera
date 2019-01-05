package unipd.se18.ocrcamera.forum.viewmodels;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import unipd.se18.ocrcamera.forum.RequestManager;
import unipd.se18.ocrcamera.forum.models.Post;

/**
 * ViewModel class for adding a post to the forum
 * (architecture used: Model - View - ViewModel)
 * @author Pietro Prandini (g2)
 */
public class AddPost_VM implements AddPostsMethods {
    /**
     * String used for logs
     */
    private String TAG = "AddPost_VM -> ";

    /**
     * Key for requesting to add a new post
     * (used by the server that hosts the forum)
     */
    private final String KEY_ADD_POST_REQUEST = "c";

    /**
     * Key for indicating the content of the post
     * (used by the server that hosts the forum)
     */
    private final String KEY_JSON_POST_CONTENT = "jPost";

    /**
     * Listener useful for communicating with the View
     */
    public interface addPostListener {
        /**
         * Notifies when a post is correctly added
         * @param response The response of the server
         */
        void onPostAdded(String response);

        /**
         * Notifies a connection problem to the network
         * @param error The error parsed
         */
        void onConnectionFailed(String error);

        /**
         * Notifies a fail of a sending parameters process addressed to the server
         * @param error The error parsed
         */
        void onParametersSendingFailed(String error);
    }

    /**
     * Sets The listener useful for communicating with the View
     * @param operationListener The instance of the listener useful for communicating with the view
     */
    public void setAddPostListener(AddPost_VM.addPostListener operationListener) {
        this.operationListener = operationListener;
    }

    /**
     * The instance of the listener useful for communicating with the view
     */
    private addPostListener operationListener;

    /**
     * Keys of the JSON strings value for a post
     */
    public enum JSONPostKey {
        ID("ID"),
        TITLE("title"),
        MESSAGE("message"),
        DATE("date"),
        LIKES("likes"),
        COMMENTS("comments"),
        AUTHOR("author");

        public String value;

        /**
         * Defines an object of type JSONPostKey
         * @param value The value of the key for the JSON format of the post
         */
        JSONPostKey(String value){ this.value = value; }
    }

    /**
     * Adds a post to the forum
     * @param context The reference of the activity/fragment that calls this method
     * @param title The new post's title
     * @param message The new post's message
     * @author Pietro Prandini (g2)
     */
    @Override
    public void addPostToForum(final Context context, String title, String message) {
        Log.i(TAG,"addPostToForum");

        // Sets up the manager useful for adding posts
        RequestManager postManager = new RequestManager();
        ArrayList<RequestManager.Parameter> postManagerParameters = new ArrayList<>();

        // Sets up the add post request parameter
        RequestManager.Parameter addPostParameter =
                new RequestManager.Parameter(
                        KEY_ADD_POST_REQUEST,
                        RequestManager.RequestType.ADD_POST.value
                );
        postManagerParameters.add(addPostParameter);

        // Formats the post in JSON format
        String JSONPostContent = getJSONPost(title, message);

        // Sets up the post content parameter
        RequestManager.Parameter postContentParameter =
                new RequestManager.Parameter(
                        KEY_JSON_POST_CONTENT,
                        JSONPostContent
                );
        postManagerParameters.add(postContentParameter);

        // Sets up the manager worker listener
        postManager.setOnRequestFinishedListener(new RequestManager.RequestManagerListener() {
            @Override
            public void onRequestFinished(String response) {
                // Post added
                Log.d(TAG,"onRequestFinished -> response: " + response);
                operationListener.onPostAdded(response);
            }

            @Override
            public void onConnectionFailed(String message) {
                // Connection problem
                Log.d(TAG,"onConnectionFailed -> message: " + message);
                operationListener.onConnectionFailed(message);
            }

            @Override
            public void onParametersSendingFailed(String message) {
                // Parameters not sent correctly
                Log.d(TAG,"onParametersSendingFailed -> message: " + message);
                operationListener.onParametersSendingFailed(message);
            }
        });

        // Sends the request
        postManager.sendRequest(context,postManagerParameters);
    }

    /**
     * Get a JSON string having the title and the message
     * @param title The title of the post
     * @param message The message of the post
     * @return The JSON string that represents the forum posts
     * @author Pietro Prandini (g2)
     */
    private String getJSONPost(String title, String message) {
        // Prepares the post's data
        Date today = new Date();
        String author = "Anon"; //TODO Retrieves the author from the login

        // Creates a post
        Post newPost = new Post(title, message, today, author);
        JSONObject JSONPost = new JSONObject();

        // Puts the values of the post in the JSON object
        try {
            JSONPost.put(JSONPostKey.ID.value, newPost.getID());
            JSONPost.put(JSONPostKey.TITLE.value, newPost.getTitle());
            JSONPost.put(JSONPostKey.MESSAGE.value, newPost.getMessage());
            JSONPost.put(JSONPostKey.DATE.value, newPost.getDate());
            JSONPost.put(JSONPostKey.LIKES.value, newPost.getLikes());
            JSONPost.put(JSONPostKey.COMMENTS.value, newPost.getComments());
            JSONPost.put(JSONPostKey.AUTHOR.value, newPost.getAuthor());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return newPost.toString();
    }
}
