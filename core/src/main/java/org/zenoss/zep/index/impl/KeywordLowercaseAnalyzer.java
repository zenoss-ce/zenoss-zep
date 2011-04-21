/*
 * Copyright (C) 2010, Zenoss Inc.  All Rights Reserved.
 */
package org.zenoss.zep.index.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 * Lower-case version of {@link KeywordAnalyzer}.
 */
public class KeywordLowercaseAnalyzer extends Analyzer {
    private static class SavedStreams {
        Tokenizer source;
        TokenStream result;
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        SavedStreams streams = (SavedStreams) getPreviousTokenStream();
        if (streams == null) {
            streams = new SavedStreams();
            streams.source = new KeywordTokenizer(reader);
            streams.result = new LowerCaseFilter(streams.source);
            setPreviousTokenStream(streams);
        }
        else {
            streams.source.reset(reader);
        }
        return streams.result;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream ts = new KeywordTokenizer(reader);
        ts = new LowerCaseFilter(ts);
        return ts;
    }
}