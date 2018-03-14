package edu.kennesaw.cs.core;

import edu.kennesaw.cs.readers.Document;
import edu.kennesaw.cs.readers.ReadCranfieldData;
import edu.kennesaw.cs.readers.StopWords;

import java.util.*;
import java.util.regex.Matcher;
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

    //variables defined when program starts so they won't keep getting initialized when going through each query
    Map<String, List<Integer>> invertedIndex = new HashMap<>();
    Map<String, Double> invertedIDFIndex = new HashMap<>();
    Map<Integer ,List<String>> listOfDocTokens = new HashMap<>();
    List<StopWords> stopWordsArray = ReadCranfieldData.stopWords();
    Map<Integer, Vector<Double>> tfPerDoc = new HashMap<>();

    public void init() {}

    //craete tokens from the body and title and removing duplicates
    public String[] tokenize(String title, String body) {
        Set<String> tokenizeIndex = new HashSet<>();
        Collections.addAll(tokenizeIndex, body.toLowerCase().split(" "));
        ArrayList<String> improveTokens = new ArrayList<>(tokenizeIndex);
        Set<String> finalTokenizeIndex = finalTokenList(improveTokens);
        return finalTokenizeIndex.toArray(new String[finalTokenizeIndex.size()]);
    }

    //getting the document body and title and begin process of adding token to index
    public void addToIndex(Document document) {
        String[] tokens = tokenize(document.getTitle(), document.getBody());
        for (String token : tokens) {
            addTokenToIndex(token, document.getId());
        }
    }

    public void createHelperVariables(int numOfDocs)
    {
        idfTokenList(numOfDocs);
        listOfDocumentTokensList(numOfDocs);
        convertDocumentToVector();
    }

    //check whether token exists in inverted index, if so add docId to token, if not then add token with docId
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

    //method to begin the process of searching for document that matches query using tf/idf with cosine simularity
    public List<Integer> search(String query) {
        String[] queryTokens = removeNotIndexTokens(query);
        List<Integer> mergedDocIds = new ArrayList<>();
        Map<Integer, Double> crossProductList = new HashMap<>();
        if (queryTokens.length == 0)
            return mergedDocIds;
        if (queryTokens.length == 1)
            mergedDocIds = invertedIndex.get(queryTokens[0]);
        else
            mergedDocIds = rankedDocuments(queryTokens, crossProductList);

        return mergedDocIds;
    }

    //https://janav.wordpress.com/2013/10/27/tf-idf-and-cosine-similarity/
    //returning the ranked documents for each query that are greater than 0
    private List<Integer> rankedDocuments(String[] queryTokens, Map<Integer, Double> crossProductList)
    {
        Vector<Double> vectorQuery = convertQueryToVector(queryTokens);
        List<Integer> mergedDocIds;
        int index = 1;
        int size = listOfDocTokens.size();
        while(index <= size)
        {
            double sum = dotProduct(vectorQuery, tfPerDoc.get(index));
            if(sum > 0)
            {
                crossProductList.put(index, sum);
            }
            index++;
        }

        mergedDocIds = crossProductList.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Collections.reverse(mergedDocIds);

        return mergedDocIds;
    }

    //method to compute dotProduct between vector query and vector documnet
    private double dotProduct(Vector<Double> vectorQuery, Vector<Double> vectorDocument) {
        double sum = 0;
        double sumOfVectorQuery = 0;
        double sumofVectorDocument = 0;
        for (int i = 0; i < vectorQuery.size(); i++) {
            sum += vectorQuery.get(i) * vectorDocument.get(i);
            sumOfVectorQuery += Math.pow(vectorQuery.get(i), 2);
            sumofVectorDocument += Math.pow(vectorDocument.get(i), 2);
        }

        return (sum / (Math.sqrt(sumOfVectorQuery) * Math.sqrt(sumofVectorDocument)));
    }

    //calculate tf/idf for each token in document and adding that value to a vector
    private void convertDocumentToVector()
    {
        Vector<Double> temp;
        List<String> invertedKeyList = new ArrayList<>(invertedIndex.keySet());
        int index = 1;
        int size = listOfDocTokens.size();
        while(index <= size)
        {
            temp = new Vector<>(Collections.nCopies(invertedIndex.size(), 0.0));
            int tempSize = listOfDocTokens.get(index).size();
            for (int i = 0; i < tempSize; i++)
            {
                int tempLocation = invertedKeyList.indexOf(listOfDocTokens.get(index).get(i));
                temp.set(tempLocation, (1.0/tempSize) * invertedIDFIndex.get(listOfDocTokens.get(index).get(i)));
            }
            tfPerDoc.put(index, temp);
            index++;
        }
    }

    //calculate tf/idf for each token in query and adding that value to a vector
    private Vector<Double> convertQueryToVector(String[] queryTokens)
    {
        Vector<Double> temp = new Vector<>(Collections.nCopies(invertedIndex.size(), 0.0));
        List<String> invertedKeyList = new ArrayList<>(invertedIndex.keySet());
        for(int i = 0; i < queryTokens.length; i++)
        {
            int tempLocation = invertedKeyList.indexOf(queryTokens[i]);
            temp.set(tempLocation, (1.0/queryTokens.length) * invertedIDFIndex.get(queryTokens[i]));
        }

        return temp;
    }
    //creating a map of each document with list of tokens in that document
    private void listOfDocumentTokensList(int listOfDocSize)
    {
        for(int i = 1; i <= listOfDocSize; i++)
        {
            List<String> listOfWords = new ArrayList<>();
            for(Map.Entry<String, List<Integer>> token : invertedIndex.entrySet())
            {
                if(token.getValue().contains(i))
                {
                    listOfWords.add(token.getKey());
                }
            }
            listOfDocTokens.put(i, listOfWords);
        }
    }

    //helper method to compute idf and adding it to a map
    private void idfTokenList (int listOfDocSize)
    {
        for (Map.Entry<String, List<Integer>> token : invertedIndex.entrySet()) {
            invertedIDFIndex.put(token.getKey(), calculateIDF(listOfDocSize, token.getValue().size()));
        }
    }

    //method to compute idf
    private Double calculateIDF(int listOfDocSize, int docIdsListSize)
    {
        return (Math.log10(listOfDocSize / docIdsListSize));
    }

    /*
    Ignore terms in query that are not in Index
    didn't change this at all
     */
    private String[] removeNotIndexTokens(String split) {
        ArrayList<String> improveTokens = new ArrayList<>(Arrays.asList(split.toLowerCase().split(" ")));
        Set<String> finalTokenizeIndex = finalTokenList(improveTokens);
        List<String> indexedTokens = new ArrayList<>();
        for (String token : finalTokenizeIndex) {
            if (invertedIndex.containsKey(token)) indexedTokens.add(token);
        }
        return indexedTokens.toArray(new String[indexedTokens.size()]);
    }

    //different helper methods that inverted token list goes through
    private Set<String> finalTokenList(ArrayList<String> tokenList)
    {
        removeSpecialCharacterInsideToken(tokenList);
        specialCharactersRemoval(tokenList);
        stopWords(tokenList);
        normalizeTokens(tokenList);
        removeSingleCharacters(tokenList);
        removeNumbers(tokenList);
        removeEmptyValues(tokenList);
        //synonyms(tokenList);
        Set<String> finalTokenizeIndex = new HashSet<>(tokenList);
        return finalTokenizeIndex;
    }

    //didn't get a chance to implement this into the program
    private void synonyms(ArrayList<String> tokenList)
    {

    }

    // if there are empty token strings
    private void removeEmptyValues(ArrayList<String> tokenList) {
        ArrayList<String> duplicateList = new ArrayList<>(tokenList);
        for (String token : duplicateList)
        {
            if (token.length() == 0)
            {
                tokenList.remove(token);
            }
            //(token.matches("[0-9]+") && token.length() > 0)
        }
    }

    //removing just 1 letter words
    private void removeSingleCharacters(ArrayList<String> tokenList)
    {
        ArrayList<String> duplicateList = new ArrayList<>(tokenList);
        for (String token : duplicateList)
        {
            if (token.length() == 1 &&  token.matches("[a-z]"))
            {
                tokenList.remove(token);
            }
        }
    }

    //removing numbers
    private void removeNumbers(ArrayList<String> tokenList)
    {
        ArrayList<String> duplicateList = new ArrayList<>(tokenList);
        String regex = "\\d+";
        for(String token : duplicateList)
        {
            if (token.matches(regex))
            {
                tokenList.remove(token);
            }
        }
    }

    //checking stop words document and if token is a stop word, removing it from the tokenlist
    private void stopWords(ArrayList<String> tokenList)
    {
        Set<String> stopWordsSet  = new HashSet<>(Arrays.asList(stopWordsArray.get(0).getwords().split(" ")));

        for(String stopWord : stopWordsSet)
        {
            if (tokenList.contains(stopWord))
            {
                tokenList.remove(stopWord);
            }
        }
    }

    // checks if there are special charater in the beginning or end of the word
    private void specialCharactersRemoval(ArrayList<String> tokenList)
    {
        ArrayList<String> duplicateList = new ArrayList<String>(tokenList);
        char[] specialCharacters = {'.', '?', ',', '!', '/',  '=', '(', ')', '*', '#', '$', '%', '\"', '\'', '-', '+', ':', '\\'};
        for(int i = 0; i < duplicateList.size(); i++)
        {
            boolean changeToken = false;
            String token = duplicateList.get(i);
            String scRemovalToken = token;
            Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(token);
            boolean b = m.find();
            if (b)
            {
                for(int j = 0; j < specialCharacters.length; j++)
                {
                    if(token.length() > 0 && token.charAt(0) == specialCharacters[j])
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
            }
            if (changeToken)
            {
                tokenList.remove(token);
                tokenList.add(scRemovalToken);
            }
        }
    }

    /***
     * uses some advice from this link:
     *      https://stackoverflow.com/questions/4283351/how-to-replace-special-characters-in-a-string
     *      https://stackoverflow.com/questions/1795402/java-check-a-string-if-there-is-a-special-character-in-it
     * @param tokenList
     */
    //checks if there are special character inside the word.  if so, remove it and split the word becuase of space and add it to token list
    private void removeSpecialCharacterInsideToken(ArrayList<String> tokenList)
    {
        ArrayList<String> duplicateList = new ArrayList<>(tokenList);
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
    }

    //if tokens ends in a s, remove just the s from the token.
    private void normalizeTokens(ArrayList<String> tokenList)
    {
        ArrayList<String> duplicateList = new ArrayList<>(tokenList);
        String[] endsWithList = {"s"};
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
    }
}