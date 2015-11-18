
package com.ibaguo.nlp.seg.CRF;

import java.util.*;

import com.ibaguo.nlp.MyNLP;
import com.ibaguo.nlp.algoritm.Viterbi;
import com.ibaguo.nlp.corpus.tag.Nature;
import com.ibaguo.nlp.dictionary.CoreDictionary;
import com.ibaguo.nlp.dictionary.CoreDictionaryTransformMatrixDictionary;
import com.ibaguo.nlp.dictionary.other.CharTable;
import com.ibaguo.nlp.model.CRFSegmentModel;
import com.ibaguo.nlp.model.crf.Table;
import com.ibaguo.nlp.model.trigram.CharacterBasedGenerativeModel;
import com.ibaguo.nlp.seg.CharacterBasedGenerativeModelSegment;
import com.ibaguo.nlp.seg.Segment;
import com.ibaguo.nlp.seg.common.Term;
import com.ibaguo.nlp.seg.common.Vertex;
import com.ibaguo.nlp.utility.CharacterHelper;



public class CRFSegment extends CharacterBasedGenerativeModelSegment
{
	public static void main(String[] args) {
		List<Term> terms = new CRFSegment().segSentence("类CompilerString是公共的, 应在名为 CompilerString.java 的文件中声明".toCharArray());
		System.out.println(terms);
	}

    @Override
    protected List<Term> segSentence(char[] sentence)
    {
        if (sentence.length == 0) return Collections.emptyList();
        char[] sentenceConverted = CharTable.convert(sentence);
        Table table = new Table();
        table.v = atomSegmentToTable(sentenceConverted);
        CRFSegmentModel.crfModel.tag(table);
        List<Term> termList = new LinkedList<Term>();
        if (MyNLP.Config.DEBUG)
        {
            System.out.println("CRF标注结果");
            System.out.println(table);
        }
        int offset = 0;
        for (int i = 0; i < table.v.length; offset += table.v[i][1].length(), ++i)
        {
            String[] line = table.v[i];
            switch (line[2].charAt(0))
            {
                case 'B':
                {
                    int begin = offset;
                    while (table.v[i][2].charAt(0) != 'E')
                    {
                        offset += table.v[i][1].length();
                        ++i;
                        if (i == table.v.length)
                        {
                            break;
                        }
                    }
                    if (i == table.v.length)
                    {
                        termList.add(new Term(new String(sentence, begin, offset - begin), null));
                    }
                    else
                        termList.add(new Term(new String(sentence, begin, offset - begin + table.v[i][1].length()), null));
                }
                break;
                default:
                {
                    termList.add(new Term(new String(sentence, offset, table.v[i][1].length()), null));
                }
                break;
            }
        }

        if (config.speechTagging)
        {
            ArrayList<Vertex> vertexList = new ArrayList<Vertex>(termList.size() + 1);
            vertexList.add(Vertex.B);
            for (Term term : termList)
            {
                CoreDictionary.Attribute attribute = CoreDictionary.get(term.word);
                if (attribute == null) attribute = new CoreDictionary.Attribute(Nature.nz);
                else term.nature = attribute.nature[0];
                Vertex vertex = new Vertex(term.word, attribute);
                vertexList.add(vertex);
            }
//            // 数字识别
//            if (config.numberQuantifierRecognize)
//            {
//                mergeNumberQuantifier(vertexList, null, config);
//            }
            Viterbi.compute(vertexList, CoreDictionaryTransformMatrixDictionary.transformMatrixDictionary);
            int i = 0;
            for (Term term : termList)
            {
                if (term.nature != null) term.nature = vertexList.get(i + 1).getNature();
                ++i;
            }
        }
        return termList;
    }

    public static List<String> atomSegment(char[] sentence)
    {
        List<String> atomList = new ArrayList<String>(sentence.length);
        final int maxLen = sentence.length - 1;
        final StringBuilder sbAtom = new StringBuilder();
        out:
        for (int i = 0; i < sentence.length; i++)
        {
            if (sentence[i] >= '0' && sentence[i] <= '9')
            {
                sbAtom.append(sentence[i]);
                if (i == maxLen)
                {
                    atomList.add(sbAtom.toString());
                    sbAtom.setLength(0);
                    break;
                }
                char c = sentence[++i];
                while (c == '.' || c == '%' || (c >= '0' && c <= '9'))
                {
                    sbAtom.append(sentence[i]);
                    if (i == maxLen)
                    {
                        atomList.add(sbAtom.toString());
                        sbAtom.setLength(0);
                        break out;
                    }
                    c = sentence[++i];
                }
                atomList.add(sbAtom.toString());
                sbAtom.setLength(0);
                --i;
            }
            else if (CharacterHelper.isEnglishLetter(sentence[i]))
            {
                sbAtom.append(sentence[i]);
                if (i == maxLen)
                {
                    atomList.add(sbAtom.toString());
                    sbAtom.setLength(0);
                    break;
                }
                char c = sentence[++i];
                while (CharacterHelper.isEnglishLetter(c))
                {
                    sbAtom.append(sentence[i]);
                    if (i == maxLen)
                    {
                        atomList.add(sbAtom.toString());
                        sbAtom.setLength(0);
                        break out;
                    }
                    c = sentence[++i];
                }
                atomList.add(sbAtom.toString());
                sbAtom.setLength(0);
                --i;
            }
            else
            {
                atomList.add(String.valueOf(sentence[i]));
            }
        }

        return atomList;
    }

    public static String[][] atomSegmentToTable(char[] sentence)
    {
        String table[][] = new String[sentence.length][3];
        int size = 0;
        final int maxLen = sentence.length - 1;
        final StringBuilder sbAtom = new StringBuilder();
        out:
        for (int i = 0; i < sentence.length; i++)
        {
            if (sentence[i] >= '0' && sentence[i] <= '9')
            {
                sbAtom.append(sentence[i]);
                if (i == maxLen)
                {
                    table[size][0] = "M";
                    table[size][1] = sbAtom.toString();
                    ++size;
                    sbAtom.setLength(0);
                    break;
                }
                char c = sentence[++i];
                while (c == '.' || c == '%' || (c >= '0' && c <= '9'))
                {
                    sbAtom.append(sentence[i]);
                    if (i == maxLen)
                    {
                        table[size][0] = "M";
                        table[size][1] = sbAtom.toString();
                        ++size;
                        sbAtom.setLength(0);
                        break out;
                    }
                    c = sentence[++i];
                }
                table[size][0] = "M";
                table[size][1] = sbAtom.toString();
                ++size;
                sbAtom.setLength(0);
                --i;
            }
            else if (CharacterHelper.isEnglishLetter(sentence[i]))
            {
                sbAtom.append(sentence[i]);
                if (i == maxLen)
                {
                    table[size][0] = "W";
                    table[size][1] = sbAtom.toString();
                    ++size;
                    sbAtom.setLength(0);
                    break;
                }
                char c = sentence[++i];
                while (CharacterHelper.isEnglishLetter(c))
                {
                    sbAtom.append(sentence[i]);
                    if (i == maxLen)
                    {
                        table[size][0] = "W";
                        table[size][1] = sbAtom.toString();
                        ++size;
                        sbAtom.setLength(0);
                        break out;
                    }
                    c = sentence[++i];
                }
                table[size][0] = "W";
                table[size][1] = sbAtom.toString();
                ++size;
                sbAtom.setLength(0);
                --i;
            }
            else
            {
                table[size][0] = table[size][1] = String.valueOf(sentence[i]);
                ++size;
            }
        }

        return resizeArray(table, size);
    }

    
    private static String[][] resizeArray(String[][] array, int size)
    {
        String[][] nArray = new String[size][];
        System.arraycopy(array, 0, nArray, 0, size);
        return nArray;
    }

    @Override
    public Segment enableNumberQuantifierRecognize(boolean enable)
    {
        throw new UnsupportedOperationException("暂不支持");
//        enablePartOfSpeechTagging(enable);
//        return super.enableNumberQuantifierRecognize(enable);
    }
}
