/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.beam.learning.katas.coretransforms.cogroupbykey;

import static org.apache.beam.sdk.values.TypeDescriptors.kvs;
import static org.apache.beam.sdk.values.TypeDescriptors.strings;

import org.apache.beam.learning.katas.util.Log;
import org.apache.beam.repackaged.core.org.apache.commons.lang3.StringUtils;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;

public class Task {

  public static void main(String[] args) {
    PipelineOptions options = PipelineOptionsFactory.fromArgs(args).create();
    Pipeline pipeline = Pipeline.create(options);

    PCollection<String> fruits =
        pipeline.apply("Fruits",
            Create.of("apple", "banana", "cherry")
        );

    PCollection<String> countries =
        pipeline.apply("Countries",
            Create.of("australia", "brazil", "canada")
        );

    PCollection<String> output = applyTransform(fruits, countries);

    output.apply(Log.ofElements());

    pipeline.run();
  }

  static PCollection<String> applyTransform(
      PCollection<String> fruits, PCollection<String> countries) {

    PCollection<KV<String,String>> fruitsKV = fruits.apply("fruits", ParDo.of(new CreateKeyValueOfWord()));
    PCollection<KV<String,String>> countriesKV = countries.apply("countries",ParDo.of(new CreateKeyValueOfWord()));

    final TupleTag<String> fruitTags =  new TupleTag<>();
    final TupleTag<String> countryTags =  new TupleTag<>();

    PCollection<KV<String, CoGbkResult>> results = KeyedPCollectionTuple.of(fruitTags, fruitsKV)
            .and(countryTags, countriesKV)
            .apply(CoGroupByKey.create());

    return results.apply(ParDo.of(new DoFn<KV<String, CoGbkResult>, String>() {

      @ProcessElement
      public void processElement(@Element KV<String, CoGbkResult> keyValue, OutputReceiver<String> op) {

        String alphabet = keyValue.getKey();
        String fruitName = keyValue.getValue().getOnly(fruitTags);
        String countryName = keyValue.getValue().getOnly(countryTags);
        WordsAlphabet wordsAlphabet = new WordsAlphabet(alphabet, fruitName, countryName);
        op.output(wordsAlphabet.toString());
      }
    }));


  }

  static  class CreateKeyValueOfWord extends DoFn<String, KV<String,String>> {

    @ProcessElement
    public void processElement(@Element String word, OutputReceiver<KV<String,String>> outputReceiver) {
      if (StringUtils.isNotEmpty(word)) {
        outputReceiver.output(KV.of(word.substring(0,1), word));
      }
    }
  }

}