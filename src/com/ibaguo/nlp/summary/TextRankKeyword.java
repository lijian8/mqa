package com.ibaguo.nlp.summary;


import java.util.*;

import com.ibaguo.nlp.seg.common.Term;
import com.ibaguo.nlp.tokenizer.StandardTokenizer;


public class TextRankKeyword extends KeywordExtractor
{
    
    int nKeyword = 10;
    
    final static float d = 0.85f;
    
    final static int max_iter = 200;
    final static float min_diff = 0.001f;

    
    public static List<String> getKeywordList(String document, int size)
    {
        TextRankKeyword textRankKeyword = new TextRankKeyword();
        textRankKeyword.nKeyword = size;
        return textRankKeyword.getKeyword(document);
    }

    public List<String> getKeyword(String content)
    {
        List<Map.Entry<String, Float>> entryList = rankWordScore(content);
//        System.out.println(entryList);
        int limit = Math.min(nKeyword, entryList.size());
        List<String> result = new ArrayList<String>(limit);
        for (int i = 0; i < limit; ++i)
        {
            result.add(entryList.get(i).getKey()) ;
        }
        return result;
    }

    public static List<Map.Entry<String, Float>> rankWordScore(String content) {
		List<Term> termList = StandardTokenizer.segment(content);
        List<String> wordList = new ArrayList<String>();
        for (Term t : termList)
        {
            if (shouldInclude(t))
            {
                wordList.add(t.word);
            }
        }
        Map<String, Set<String>> words = new TreeMap<String, Set<String>>();
        Queue<String> que = new LinkedList<String>();
        for (String w : wordList)
        {
            if (!words.containsKey(w))
            {
                words.put(w, new TreeSet<String>());
            }
            que.offer(w);
            if (que.size() > 5)
            {
                que.poll();
            }

            for (String w1 : que)
            {
                for (String w2 : que)
                {
                    if (w1.equals(w2))
                    {
                        continue;
                    }

                    words.get(w1).add(w2);
                    words.get(w2).add(w1);
                }
            }
        }
//        System.out.println(words);
        Map<String, Float> score = new HashMap<String, Float>();
        for (int i = 0; i < max_iter; ++i)
        {
            Map<String, Float> m = new HashMap<String, Float>();
            float max_diff = 0;
            for (Map.Entry<String, Set<String>> entry : words.entrySet())
            {
                String key = entry.getKey();
                Set<String> value = entry.getValue();
                m.put(key, 1 - d);
                for (String element : value)
                {
                    int size = words.get(element).size();
                    if (key.equals(element) || size == 0) continue;
                    m.put(key, m.get(key) + d / size * (score.get(element) == null ? 0 : score.get(element)));
                }
                max_diff = Math.max(max_diff, Math.abs(m.get(key) - (score.get(key) == null ? 0 : score.get(key))));
            }
            score = m;
            if (max_diff <= min_diff) break;
        }
        List<Map.Entry<String, Float>> entryList = new ArrayList<Map.Entry<String, Float>>(score.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Float>>()
        {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2)
            {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
		return entryList;
	}

}
