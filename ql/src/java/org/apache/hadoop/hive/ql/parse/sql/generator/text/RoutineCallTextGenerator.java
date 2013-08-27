/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.parse.sql.generator.text;

import org.antlr33.runtime.tree.CommonTree;
import org.apache.hadoop.hive.ql.parse.sql.SqlParseException;
import org.apache.hadoop.hive.ql.parse.sql.TranslateContext;

/**
 * generate like Function(...) .<br>
 * RoutineCallTextGenerator.
 *
 */
public class RoutineCallTextGenerator extends BaseTextGenerator {

  @Override
  protected String textGenerate(CommonTree root, TranslateContext context) throws Exception {
    if (!(root.getChild(0) instanceof org.antlr33.runtime.tree.CommonTree)) {
      throw new SqlParseException("illegal sql AST node:" + root.getChild(0));
    }
    CommonTree op0 = (CommonTree) root.getChild(0);
    QueryTextGenerator qr0 = TextGeneratorFactory.getTextGenerator(op0);
    if (!(root.getChild(1) instanceof org.antlr33.runtime.tree.CommonTree)) {
      throw new SqlParseException("illegal sql AST node:" + root.getChild(1));
    }
    CommonTree op1 = (CommonTree) root.getChild(1);
    QueryTextGenerator qr1 = TextGeneratorFactory.getTextGenerator(op1);
    return qr0.textGenerateQuery(op0, context) + "(" + qr1.textGenerateQuery(op1, context) + ")";
  }

}
