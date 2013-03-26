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
package org.apache.hadoop.hive.ql.parse.sql.transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr33.runtime.tree.CommonTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.parse.sql.SqlASTNode;
import org.apache.hadoop.hive.ql.parse.sql.SqlXlateException;
import org.apache.hadoop.hive.ql.parse.sql.TranslateContext;
import org.apache.hadoop.hive.ql.parse.sql.transformer.fb.FilterBlock;
import org.apache.hadoop.hive.ql.parse.sql.transformer.fb.FilterBlockContext;
import org.apache.hadoop.hive.ql.parse.sql.transformer.fb.FilterBlockUtil;
import org.apache.hadoop.hive.ql.parse.sql.transformer.fb.QueryBlock;
import org.apache.hadoop.hive.ql.parse.sql.transformer.fb.SubQFilterBlock;

import br.com.porcelli.parser.plsql.PantheraParser_PLSQLParser;

/**
 * Transform filter block tree with every QueryInfo.
 * FilterBlockTransformer.
 *
 */
public class SubQUnnestTransformer extends BaseSqlASTTransformer {

  private static final Log LOG = LogFactory.getLog(SubQUnnestTransformer.class);
  SqlASTTransformer tf;

  public SubQUnnestTransformer(SqlASTTransformer tf) {
    this.tf = tf;
  }

  @Override
  public void transform(SqlASTNode tree, TranslateContext context) throws SqlXlateException {
    tf.transformAST(tree, context);
    // this.transformQInfo(tree, context);
    this.transformQInfoDeepFirst(tree, context);
  }

  /**
   * Transform SQL AST tree.<br>
   * If the transformation need to be processed with every QueryInfo by multi-threads, overload the
   * method.(TODO can't support, because qinfo should be travel by depth-first for subquery
   * inlineview)
   *
   * @deprecated
   * @param tree
   * @param context
   * @throws SqlXlateException
   *
   */
  @Deprecated
  void transformQInfo(SqlASTNode tree, TranslateContext context) throws SqlXlateException {
    for (QueryInfo qf : context.getqInfoList()) {
      this.transformFilterBlock(qf, context);
    }
  }

  /**
   * Transform inline view firstly.
   * @param tree
   * @param context
   * @throws SqlXlateException
   */
  void transformQInfoDeepFirst(SqlASTNode tree, TranslateContext context) throws SqlXlateException {

    QueryInfo qInfo = context.getQInfoRoot();
    transformQInfoDeepFirst(qInfo, context);


  }

  void transformQInfoDeepFirst(QueryInfo qf, TranslateContext context) throws SqlXlateException {
    List<List<String>> selectListStrList = new ArrayList<List<String>>();
    List<CommonTree> selectList = new ArrayList<CommonTree>();
    for (QueryInfo qinfo : qf.getChildren()) {
      selectListStrList.add(qinfo.getSelectList());
      transformQInfoDeepFirst(qinfo, context);
      selectList.add(qinfo.getSelectKeyForThisQ());
    }
    if (!qf.isQInfoTreeRoot()) {
      this.rebuildSelect(qf.getSelectKeyForThisQ(), selectListStrList, selectList);
      this.transformFilterBlock(qf, context);
    } else {

    }
  }


  void transformFilterBlock(QueryInfo qf, TranslateContext context) throws SqlXlateException {
    if (!this.hasSubQuery(qf)) {
      LOG.info("skip subq transform:" + qf.toStringTree());
      return;
    }
    FilterBlock fb = qf.getFilterBlockTreeRoot();
    if (!(fb instanceof QueryBlock)) {
      throw new SqlXlateException("Error FilterBlock tree" + fb.toStringTree());
    }

    fb.process(new FilterBlockContext(qf), context);
    qf.setSelectKeyForThisQ(fb.getTransformedNode());
  }

  boolean hasSubQuery(QueryInfo qf) {
    FilterBlock fb = qf.getFilterBlockTreeRoot();
    Set<Boolean> result = new HashSet<Boolean>();
    this.isSubQFilterBlock(fb, result);
    if (result.contains(true)) {
      return true;
    }
    return false;
  }

  void isSubQFilterBlock(FilterBlock fb, Set<Boolean> result) {
    if (fb instanceof SubQFilterBlock) {
      result.add(true);
      return;
    }
    for (FilterBlock child : fb.getChildren()) {
      isSubQFilterBlock(child, result);
    }
  }

  /**
   * rebuild SELECT_LIST, GROUP, ORDER_BY with column alias of unnested inlineview
   * TODO WHERE
   * @param select
   * @param selectListStrList
   * @param selectList
   */
  private void rebuildSelect(CommonTree select, List<List<String>> selectListStrList,
      List<CommonTree> selectList) {

    // SELECT_LIST
    CommonTree selectListNode = (CommonTree) select
        .getFirstChildWithType(PantheraParser_PLSQLParser.SELECT_LIST);
    if (selectListNode == null) {
      return;
    }
    for (int i = 0; i < selectListNode.getChildCount(); i++) {
      this
          .rebuildAnyElement((CommonTree) selectListNode.getChild(i), selectListStrList, selectList);
    }

    // GROUP_BY
    this.rebuildAnyElement((CommonTree) select
        .getFirstChildWithType(PantheraParser_PLSQLParser.SQL92_RESERVED_GROUP), selectListStrList,
        selectList);

    // ORDER_BY
    CommonTree subQuery = (CommonTree) select.getParent();
    if (subQuery != null) {
      CommonTree selectStatement = (CommonTree) subQuery.getParent();
      if (selectStatement != null
          && selectStatement.getType() == PantheraParser_PLSQLParser.SELECT_STATEMENT) {
        CommonTree order = (CommonTree) selectStatement
            .getFirstChildWithType(PantheraParser_PLSQLParser.SQL92_RESERVED_ORDER);
        this.rebuildAnyElement(order, selectListStrList, selectList);
      }
    }
  }

  private void rebuildAnyElement(CommonTree targetNode, List<List<String>> selectListStrList,
      List<CommonTree> selectList) {
    if (targetNode == null) {
      return;
    }
    List<CommonTree> anyElementList = new ArrayList<CommonTree>();
    FilterBlockUtil.findNode(targetNode, PantheraParser_PLSQLParser.ANY_ELEMENT, anyElementList);
    CommonTree anyElement = anyElementList.isEmpty() ? null : anyElementList.get(0);
    if (anyElement == null) {
      return;
    }
    CommonTree column;
    if (anyElement.getChildCount() == 2) {
      column = (CommonTree) anyElement.getChild(1);

    } else {
      column = (CommonTree) anyElement.getChild(0);
    }
    String originalColumnName = column.getText();
    String transformedAlias = findAlias(originalColumnName, selectListStrList, selectList);
    column.getToken().setText(transformedAlias);
  }

  private String findAlias(String originalColumnName, List<List<String>> selectListStrList,
      List<CommonTree> selectList) {
    for (int i = 0; i < selectListStrList.size(); i++) {
      List<String> strList = selectListStrList.get(i);
      CommonTree select = selectList.get(i);
      for (int j = 0; j < strList.size(); j++) {
        String str = strList.get(j);
        if (str.equals(originalColumnName)) {
          return select.getFirstChildWithType(PantheraParser_PLSQLParser.SELECT_LIST).getChild(j)
              .getChild(1).getChild(0).getText();
        }
      }
    }
    return null;
  }

}
