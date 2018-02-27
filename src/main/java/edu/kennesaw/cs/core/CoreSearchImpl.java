package edu.kennesaw.cs.core;

import edu.kennesaw.cs.readers.Document;
import edu.kennesaw.cs.readers.ReadCranfieldData;
import edu.kennesaw.cs.readers.StopWords;

import java.util.*;
import java.util.regex.Pattern;

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
        List<Integer> mergedDocIds = new ArrayList<Integer>();
        if (queryTokens.length == 0) return mergedDocIds;
        if (queryTokens.length == 1) invertedIndex.get(queryTokens[0]);
        int index = 0;
        //List<Integer> initial = invertedIndex.get(queryTokens[0]);
        Map<String, Double> initial = new HashMap<String, Double>();
        List<List<Integer>> invertedValuesList = new ArrayList<List<Integer>>(invertedIndex.values());
        Map<Integer, Integer> totalCountPerDoc = totalCountPerDocument(invertedValuesList);
        while (index < queryTokens.length) {
            //initial = mergeTwoDocIds(initial, invertedIndex.get(queryTokens[index]));
            //initial.put(queryTokens[index], calculatetfidf(queryTokens[index], totalCountPerDoc));
            if(index == 0)
            {
                initial = calculatetfidf(queryTokens[index], totalCountPerDoc);
            }
            else
            {
                Map<String, Double> temp = calculatetfidf(queryTokens[index], totalCountPerDoc);
                for(int i = 1; i <= temp.size(); i++)
                {
                    System.out.println(i);
                    double total = initial.get(String.valueOf(i)) + temp.get(String.valueOf(i));
                    initial.put(String.valueOf(i), total);
                }
            }
            index++;
        }

        List<Double> maxScore = new ArrayList<Double>(initial.values());
        double highestScore = Collections.max(maxScore);
        return sortMap(initial, highestScore);
    }

    private List<Integer> sortMap(Map<String, Double> initial, Double highestScore)
    {
        List<Integer> docIdList = new ArrayList<Integer>();
        for (String docId : initial.keySet()) {
            if (initial.get(docId).equals(highestScore)) {
                docIdList.add(Integer.parseInt(docId));
            }
        }
        return docIdList;
    }

    private Map<Integer, Integer> totalCountPerDocument(List<List<Integer>> invertedValuesList)
    {
        Map<Integer, Integer> listOfTest = new HashMap<Integer, Integer>();
        for(int i = 1; i <= 1400; i++)
        {
            int total;
            List<List<Integer>> testList2 = new ArrayList<List<Integer>>();
            for (List<Integer> docIds : invertedValuesList)
            {
                if(docIds.contains(i))
                {
                    testList2.add(docIds);
                    total = testList2.size();
                    listOfTest.put(i, total);
                }
            }
            if(testList2.size() == 0)
            {
                listOfTest.put(i, 0);
            }
        }
        return listOfTest;
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
    public List<Integer> mergeTwoDocIds(List<Integer> docList1, List<Integer> docList2) {
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
    }

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

    private Map<String, Double> calculatetfidf(String queryToken, Map<Integer, Integer> totalCountPerDoc)
    {
        Map<String, Double> tfidfMapPerQueryDoc = new HashMap<String, Double>();
        for(int i = 1; i <= totalCountPerDoc.size(); i++)
        {
            double tf = tf(queryToken, totalCountPerDoc.get(i), i);
            double idf = idf(queryToken);
            double tfidf = tfIdf(tf,idf);
            tfidfMapPerQueryDoc.put(String.valueOf(i), tfidf);
        }
        return tfidfMapPerQueryDoc;
    }

    private double tf(String term, Integer docSize, Integer docId) {
        double N = invertedIndex.get(term).contains(docId) ? 1 : 0;
        double df = docSize;
        if (df == 0)
            return 0;
        return (N / df);
    }

    private double idf(String term) {
        return Math.log(ReadCranfieldData.readDocuments().size() / invertedIndex.get(term).size());
    }

    private double tfIdf(double tf, double idf) {
        return tf*idf;
    }
}
