
package com.ibaguo.nlp.corpus.dictionary;

import static com.ibaguo.nlp.utility.Predefine.logger;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.ibaguo.nlp.corpus.document.CorpusLoader;
import com.ibaguo.nlp.corpus.document.Document;
import com.ibaguo.nlp.corpus.document.sentence.word.IWord;
import com.ibaguo.nlp.corpus.document.sentence.word.Word;
import com.ibaguo.nlp.corpus.tag.NR;
import com.ibaguo.nlp.corpus.tag.NS;
import com.ibaguo.nlp.corpus.tag.Nature;
import com.ibaguo.nlp.corpus.util.CorpusUtil;
import com.ibaguo.nlp.corpus.util.Precompiler;
import com.ibaguo.nlp.utility.Predefine;


public class NSDictionaryMaker extends CommonDictionaryMaker
{
    public NSDictionaryMaker(EasyDictionary dictionary)
    {
        super(dictionary);
    }

    @Override
    protected void addToDictionary(List<List<IWord>> sentenceList)
    {
//        logger.warning("开始制作词典");
        // 将非A的词语保存下来
        for (List<IWord> wordList : sentenceList)
        {
            for (IWord word : wordList)
            {
                if (!word.getLabel().equals(NS.Z.toString()))
                {
                    dictionaryMaker.add(word);
                }
            }
        }
        // 制作NGram词典
        for (List<IWord> wordList : sentenceList)
        {
            IWord pre = null;
            for (IWord word : wordList)
            {
                if (pre != null)
                {
                    nGramDictionaryMaker.addPair(pre, word);
                }
                pre = word;
            }
        }
    }

    @Override
    protected void roleTag(List<List<IWord>> sentenceList)
    {
        int i = 0;
        for (List<IWord> wordList : sentenceList)
        {
            Precompiler.compileWithoutNS(wordList);
            if (verbose)
            {
                System.out.print(++i + " / " + sentenceList.size() + " ");
                System.out.println("原始语料 " + wordList);
            }
            LinkedList<IWord> wordLinkedList = (LinkedList<IWord>) wordList;
            wordLinkedList.addFirst(new Word(Predefine.TAG_BIGIN, "S"));
            wordLinkedList.addLast(new Word(Predefine.TAG_END, "Z"));
            if (verbose) System.out.println("添加首尾 " + wordList);
            // 标注上文
            Iterator<IWord> iterator = wordLinkedList.iterator();
            IWord pre = iterator.next();
            while (iterator.hasNext())
            {
                IWord current = iterator.next();
                if (current.getLabel().startsWith("ns") && !pre.getLabel().startsWith("ns"))
                {
                    pre.setLabel(NS.A.toString());
                }
                pre = current;
            }
            if (verbose) System.out.println("标注上文 " + wordList);
            // 标注下文
            iterator = wordLinkedList.descendingIterator();
            pre = iterator.next();
            while (iterator.hasNext())
            {
                IWord current = iterator.next();
                if (current.getLabel().startsWith("ns") && !pre.getLabel().startsWith("ns"))
                {
                    pre.setLabel(NS.B.toString());
                }
                pre = current;
            }
            if (verbose) System.out.println("标注下文 " + wordList);
            // 标注中间
            iterator = wordLinkedList.iterator();
            IWord first = iterator.next();
            IWord second = iterator.next();
            while (iterator.hasNext())
            {
                IWord third = iterator.next();
                if (first.getLabel().startsWith("ns") && third.getLabel().startsWith("ns") && !second.getLabel().startsWith("ns"))
                {
                    second.setLabel(NS.X.toString());
                }
                first = second;
                second = third;
            }
            if (verbose) System.out.println("标注中间 " + wordList);
            // 拆分地名
            CorpusUtil.spilt(wordList);
            if (verbose) System.out.println("拆分地名 " + wordList);
            // 处理整个
            ListIterator<IWord> listIterator = wordLinkedList.listIterator();
            while (listIterator.hasNext())
            {
                IWord word = listIterator.next();
                String label = word.getLabel();
                if (label.equals(label.toUpperCase())) continue;
                if (label.startsWith("ns"))
                {
                    String value = word.getValue();
                    int longestSuffixLength = PlaceSuffixDictionary.dictionary.getLongestSuffixLength(value);
                    int wordLength = value.length() - longestSuffixLength;
                    if (longestSuffixLength == 0 || wordLength == 0)
                    {
                        word.setLabel(NS.G.toString());
                        continue;
                    }
                    listIterator.remove();
                    if (wordLength > 3)
                    {
                        listIterator.add(new Word(value.substring(0, wordLength), NS.G.toString()));
                        listIterator.add(new Word(value.substring(wordLength), NS.H.toString()));
                        continue;
                    }
                    for (int l = 1, tag = NS.C.ordinal(); l <= wordLength; ++l, ++tag)
                    {
                        listIterator.add(new Word(value.substring(l - 1, l), NS.values()[tag].toString()));
                    }
                    listIterator.add(new Word(value.substring(wordLength), NS.H.toString()));
                }
                else
                {
                    word.setLabel(NS.Z.toString());
                }
            }
            if (verbose) System.out.println("处理整个 " + wordList);
        }
    }

    public static void main(String[] args)
    {
        EasyDictionary dictionary = EasyDictionary.create("data/dictionary/2014_dictionary.txt");
        final NSDictionaryMaker nsDictionaryMaker = new NSDictionaryMaker(dictionary);
        CorpusLoader.walk("D:\\JavaProjects\\CorpusToolBox\\data\\2014\\", new CorpusLoader.Handler()
        {
            @Override
            public void handle(Document document)
            {
                nsDictionaryMaker.compute(document.getComplexSentenceList());
            }
        });
        nsDictionaryMaker.saveTxtTo("D:\\JavaProjects\\HanLP\\data\\dictionary\\place\\ns");
    }
}
