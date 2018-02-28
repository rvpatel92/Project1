package edu.kennesaw.cs.core;

import edu.kennesaw.cs.readers.Document;
import edu.kennesaw.cs.readers.ReadCranfieldData;
import edu.kennesaw.cs.readers.StopWords;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
This class is an example implementation of the CoreSearch, you can either modify or write another implementation of the Core Search.
 */

/**
 * Created by Ferosh Jacob
 * Date: 01/27/18
 * KSU: CS 7263 Text Mining
 */
public class CoreSearchImpl implements CoreSearch {


    Map<String, List<Integer>> invertedIndex = new HashMap<String, List<Integer>>();
    Map<String, Double> invertedIDFIndex = new HashMap<String, Double>();
    Map<Integer ,Double> tfDocID = new HashMap<>();
    List<StopWords> stopWordsArray = ReadCranfieldData.stopWords();

    public void init() {}
    /*
    A very simple tokenization.
    */

    public String[] tokenize(String title, String body) {
        Set<String> tokenizeIndex = new HashSet<String>();
        Collections.addAll(tokenizeIndex, title.toLowerCase().split(" "));
        Collections.addAll(tokenizeIndex, body.toLowerCase().split(" "));
        ArrayList<String> improveTokens = new ArrayList<String>(tokenizeIndex);
        Set<String> finalTokenizeIndex = finalTokenList(improveTokens);
        return finalTokenizeIndex.toArray(new String[finalTokenizeIndex.size()]);
    }

    public void addToIndex(Document document) {
        String[] tokens = tokenize(document.getTitle(), document.getBody());
        for (String token : tokens) {
            addTokenToIndex(token, document.getId());
        }
    }

    private void addTokenToIndex(String token, int docId) {

        if (invertedIndex.containsKey(token)) {
            List<Integer> docIds = invertedIndex.get(token);
            docIds.add(docId);
            Collections.sort(docIds);
            invertedIndex.put(token, docIds);
        } else {
            List<Integer> docIds = new ArrayList<Integer>();
            docIds.add(docId);
            invertedIndex.put(token, docIds);
        }
    }

    /*
    A very simple search implementation.
     */
    public List<Integer> search(String query) {
        String[] queryTokens = removeNotIndexTokens(query);
        int listOfDocSize = ReadCranfieldData.readDocuments().size();
        List<Integer> mergedDocIds = new ArrayList<Integer>();
        idfTokenList(listOfDocSize, invertedIndex);
        Map<Integer, Double> crossProductList = new HashMap<Integer, Double>();
        if (queryTokens.length == 0)
            return mergedDocIds;
        if (queryTokens.length == 1)
            mergedDocIds = invertedIndex.get(queryTokens[0]);
        else
            mergedDocIds = rankedDocuments(queryTokens, listOfDocSize, crossProductList);


        return mergedDocIds;
    }

    //https://janav.wordpress.com/2013/10/27/tf-idf-and-cosine-similarity/
    private List<Integer> rankedDocuments(String[] queryTokens, int listOfDocSize, Map<Integer, Double> crossProductList)
    {
        Vector<Double> vectorQuery = convertQueryToVector(queryTokens);
        List<Integer> mergedDocIds;
        for(int i = 1; i <= listOfDocSize; i++)
        {
            Vector<Double> temp = convertDocumentToVector(i, listOfDocSize);
            double sum = dotProduct(vectorQuery, temp);
            if(sum > 0)
            {
                crossProductList.put(i, sum);
            }
        }
        /*int index = 0;
        List<Integer> initial = invertedIndex.get(queryTokens[0]);
        while (index < queryTokens.length) {
            initial = mergeTwoDocIds(initial, invertedIndex.get(queryTokens[index]));
            index++;
        }*/

        //Integer key = Collections.max(crossProduct.entrySet(), Map.Entry.comparingByValue()).getKey();
        mergedDocIds = crossProductList.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Collections.reverse(mergedDocIds);

        return mergedDocIds;
    }


    private double dotProduct(Vector<Double> a, Vector<Double> b) {
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            sum += a.get(i) * b.get(i);
        }
        return sum;
    }

    private Vector<Double> convertDocumentToVector(int docId, int listOfDocSize)
    {
        Vector<Double> temp = new Vector<Double>(Collections.nCopies(invertedIndex.size(), 0.0));
        List<String> listOfWords = new ArrayList<String>();
        for(Map.Entry<String, List<Integer>> token : invertedIndex.entrySet())
        {
            if(token.getValue().contains(docId))
            {
                listOfWords.add(token.getKey());
            }
        }

        List<String> invertedKeyList = new ArrayList<String>(invertedIndex.keySet());
        double test = 0;
        for(int i = 0; i < listOfWords.size(); i++)
        {
            int tempLocation = invertedKeyList.indexOf(listOfWords.get(i));
            temp.set(tempLocation, (1.0/listOfWords.size()) * invertedIDFIndex.get(listOfWords.get(i)));
            test += (1.0/listOfWords.size()) * invertedIDFIndex.get(listOfWords.get(i));
        }
        return temp;
    }

    private Vector<Double> convertQueryToVector(String[] queryTokens)
    {
        Vector<Double> temp = new Vector<Double>(Collections.nCopies(invertedIndex.size(), 0.0));
        List<String> invertedKeyList = new ArrayList<String>(invertedIndex.keySet());
        for(int i = 0; i < queryTokens.length; i++)
        {
            int tempLocation = invertedKeyList.indexOf(queryTokens[i]);
            temp.set(tempLocation, (1.0/queryTokens.length) * invertedIDFIndex.get(queryTokens[i]));
        }

        return temp;
    }

    private void idfTokenList (int listOfDocSize, Map<String, List<Integer>> invertedIndex)
    {
        for (Map.Entry<String, List<Integer>> token : invertedIndex.entrySet()) {
            invertedIDFIndex.put(token.getKey(), calculateIDF(listOfDocSize, token.getValue().size()));
        }
    }

    private Double calculateIDF(int listOfDocSize, int docIdsListSize)
    {
        return (Math.log10(listOfDocSize / docIdsListSize));
    }

    /*
    Ignore terms in query that are not in Index
     */
    private String[] removeNotIndexTokens(String split) {
        ArrayList<String> improveTokens = new ArrayList<String>(Arrays.asList(split.toLowerCase().split(" ")));
        Set<String> finalTokenizeIndex = finalTokenList(improveTokens);
        List<String> indexedTokens = new ArrayList<String>();
        for (String token : finalTokenizeIndex) {
            if (invertedIndex.containsKey(token)) indexedTokens.add(token);
        }
        return indexedTokens.toArray(new String[indexedTokens.size()]);
    }

    /*
    Now its OR Merging postings!!
     */
    /*public List<Integer> mergeTwoDocIds(List<Integer> docList1, List<Integer> docList2) {
        int docIndex1 = 0;
        int docIndex2 = 0;
        List<Integer> mergedList = new ArrayList<Integer>();
        while (docIndex1 < docList1.size() && docIndex2 < docList2.size()) {
            if (docList1.get(docIndex1).intValue() == docList2.get(docIndex2).intValue()) {
                mergedList.add(docList1.get(docIndex1));
                docIndex1++;
                docIndex2++;
            } else if (docList1.get(docIndex1) < docList2.get(docIndex2)) {
                mergedList.add(docList1.get(docIndex1));
                docIndex1++;
            } else if (docList1.get(docIndex1) > docList2.get(docIndex2)) {
                mergedList.add(docList2.get(docIndex2));
                docIndex2++;
            }
        }
        return mergedList;
    }*/

    private Set<String> finalTokenList(ArrayList<String> tokenList)
    {
        removeEmptyValues(tokenList);
        specialCharactersRemoval(tokenList);
        stopWords(tokenList);
        removeSCharacterInsideToken(tokenList);
        normalizeTokens(tokenList);
        Set<String> finalTokenizeIndex = new HashSet<String>(tokenList);
        return finalTokenizeIndex;
    }

    private void removeEmptyValues(ArrayList<String> tokenList) {
        ArrayList<String> duplicateList = new ArrayList<String>(tokenList);
        for (String token : duplicateList)
        {
            if (token.length() == 0)
            {
                tokenList.remove(token);
            }
            //(token.matches("[0-9]+") && token.length() > 0)
        }
    }

    private void stopWords(ArrayList<String> tokenList)
    {
        Set<String> stopWordsSet  = new HashSet<String>(Arrays.asList(stopWordsArray.get(0).getwords().split(" ")));

        for(String stopWord : stopWordsSet)
        {
            if (tokenList.contains(stopWord))
            {
                tokenList.remove(stopWord);
            }
        }
    }

    private void specialCharactersRemoval(ArrayList<String> tokenList)
    {
        ArrayList<String> duplicateList = new ArrayList<String>(tokenList);
        char[] specialCharacters = {'.', '?', ',', '!', '/',  '=', '(', ')', '*', '#', '$', '%', '\"', '\'', '-', '+', ':', '\\'};
        for(int i = 0; i < duplicateList.size(); i++)
        {
            boolean changeToken = false;
            String token = duplicateList.get(i);
            String scRemovalToken = token;
            for(int j = 0; j < specialCharacters.length; j++)
            {
                if(token.charAt(0) == specialCharacters[j])
                {
                    scRemovalToken = token.substring(1, token.length());
                    changeToken = true;
                    if(token.length() > 1 && scRemovalToken.charAt(scRemovalToken.length()-1) == specialCharacters[j])
                    {
                        scRemovalToken = scRemovalToken.substring(0, scRemovalToken.length()-1);
                        changeToken = true;
                        break;
                    }
                }
                if(token.length() > 1 && token.charAt(token.length()-1) == specialCharacters[j])
                {
                    scRemovalToken = token.substring(0, token.length()-1);
                    changeToken = true;
                }
            }
            if (changeToken == true)
            {
                tokenList.remove(token);
                tokenList.add(scRemovalToken);
            }
        }
        removeEmptyValues(tokenList);
    }

    /***
     * uses some advice from this link:
     *      https://stackoverflow.com/questions/4283351/how-to-replace-special-characters-in-a-string
     *      https://stackoverflow.com/questions/1795402/java-check-a-string-if-there-is-a-special-character-in-it
     * @param tokenList
     */
    private void removeSCharacterInsideToken(ArrayList<String> tokenList)
    {
        ArrayList<String> duplicateList = new ArrayList<String>(tokenList);
        String thePattern = "[^A-Za-z0-9]+";
        for (String token : duplicateList)
        {
            String scRemovalToken = token;
            if (Pattern.compile(thePattern).matcher(token).find() && token.length() > 1)
            {
                token = token.replaceAll("[^a-zA-Z]+"," ");
                tokenList.remove(scRemovalToken);
                Collections.addAll(tokenList, token.toLowerCase().split(" "));
            }
        }
        removeEmptyValues(tokenList);
        specialCharactersRemoval(tokenList);
    }

    private void normalizeTokens(ArrayList<String> tokenList)
    {
        ArrayList<String> duplicateList = new ArrayList<String>(tokenList);
        String[] endsWithList = {"s", "ed", "ing", "ies"};
        for (int i = 0; i < duplicateList.size(); i++)
        {
            String token = duplicateList.get(i);
            String scRemovalToken = token;
            for(int j = 0; j < endsWithList.length; j++)
            {
                if(token.endsWith(endsWithList[j]))
                {
                    scRemovalToken = scRemovalToken.substring(0, scRemovalToken.length() - endsWithList[j].length());
                    tokenList.remove(token);
                    tokenList.add(scRemovalToken);
                }
            }
        }
        removeEmptyValues(tokenList);
    }
}
