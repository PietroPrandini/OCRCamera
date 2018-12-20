package unipd.se18.ocrcamera.inci;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import edu.gatech.gtri.bktree.BkTreeSearcher;
import edu.gatech.gtri.bktree.Metric;
import edu.gatech.gtri.bktree.MutableBkTree;

/**
 * This class corrects automatically the ocr text using a
 * dictionary of different words taken from INCI DB.
 *
 * To see how I generated the dictionary see :
 * https://github.com/frankplus/incidb/tree/master/src/main/java
 *
 * For the search of best matching words in the dictionary I used an implementation of bk-trees,
 * this way the search complexity is logarithmic instead of quadratic (shortening the processing time
 * from about 20 seconds to half a second)
 * For more information: https://github.com/gtri/bk-tree
 *
 * @author Francesco Pham
 */
public class TextAutoCorrection {

    private final String TAG = "TextAutoCorrection";

    //searcher for words in the word list with minimum distance from a given query
    private BkTreeSearcher<String> searcher;

    /**
     * Constructor which loads the word list into a bk-tree and initialize the searcher
     * @param wordList InputStream from the word list
     * @author Francesco Pham
     */
    public TextAutoCorrection(InputStream wordList){

        //open word list
        BufferedReader reader = new BufferedReader(new InputStreamReader(wordList));

        //declaring metric used for string distance
        final LevenshteinStringDistance levenshtein = new LevenshteinStringDistance();
        final Metric<String> levenshteinDistance = new Metric<String>() {
            @Override
            public int distance(String x, String y) {
                return (int) levenshtein.distance(x,y);
            }
        };

        //inizialize bk-tree
        MutableBkTree<String> bkTree = new MutableBkTree<>(levenshteinDistance);

        //add each element to the tree
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                bkTree.add(line);
            }
        }catch(IOException e){
            Log.e(TAG,"Error reading word list");
        }

        //initialize searcher
        searcher = new BkTreeSearcher<>(bkTree);
    }


    /**
     * Each word of the text is searched for a best match in the dictionary and
     * if there is a good match the word is corrected
     * @param text The text (probably taken from ocr) which has to be corrected
     * @return Corrected text
     */
    public String correctText(String text){

        //not correcting words with less than minChars characters
        final int minChars = 3;

        text = formatText(text);

        //split the text into each word containing only letters or numbers with more than minChars characters
        int lastNonAlphanumIndex = -1;
        for(int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if(!Character.isLetter(c) && !Character.isDigit(c)){
                if(i > lastNonAlphanumIndex+minChars){

                    String word = text.substring(lastNonAlphanumIndex+1,i);
                    String corrected = correctWord(word);

                    if(!corrected.equals(word)){

                        Log.d(TAG,"word "+word+" corrected with "+corrected);

                        //substitute with the word found
                        text = text.substring(0, lastNonAlphanumIndex+1) + corrected + text.substring(i);

                        //take into account the difference of length between the words
                        i += corrected.length()-word.length();
                    }
                }

                lastNonAlphanumIndex = i;
            }
        }

        //check last word
        if(text.length() > lastNonAlphanumIndex+minChars){

            String word = text.substring(lastNonAlphanumIndex+1);
            String corrected = correctWord(word);

            if(!corrected.equals(word)){

                Log.d(TAG,"word "+word+" corrected with "+corrected);

                //substitute with the word found
                text = text.substring(0, lastNonAlphanumIndex+1) + corrected;
            }
        }

        return text;
    }


    /**
     * format the text in order to increase the probability to match ingredients in the INCI DB
     * @param text Text to be formatted
     * @return Text formatted
     */
    private String formatText(String text){
        //merge the word before and after hyphen + new line (e.g. "ceta- \n ril" into "cetaryl")
        text = text.replaceAll(" *- *[\\n\\r]+ *", "");

        //ignoring case by converting all into upper case
        text = text.toUpperCase();
        return text;
    }

    /**
     * Correct a single word by searching for the most similar in word list
     * @param word The word to be corrected
     * @return Best candidate word from word list. If no words within maxDistance is found, the same word is returned.
     */
    private String correctWord(String word){

        //percentage distance below which we substitute the word with the term found in dictionary
        final double maxDistance = 0.25;

        //Searches the tree for elements whose distance satisfy maxDistance
        Set<BkTreeSearcher.Match<? extends String>> matches =
                searcher.search(word, (int) (word.length()*maxDistance));

        //find the word with minimum distance
        int minDistance = Integer.MAX_VALUE;
        String closest = "";
        for (BkTreeSearcher.Match<? extends String> match : matches){
            if(match.getDistance() < minDistance) {
                minDistance = match.getDistance();
                closest = match.getMatch();
            }
        }

        //If no words within maxDistance is found, the same word is returned.
        return closest.equals("") ? word : closest;
    }
}
