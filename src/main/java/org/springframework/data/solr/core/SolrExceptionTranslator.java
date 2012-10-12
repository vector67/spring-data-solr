/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core;

import org.apache.lucene.queryParser.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.solr.UncategorizedSolrException;

/**
 * @author Christoph Strobl
 * 
 */
public class SolrExceptionTranslator implements PersistenceExceptionTranslator {

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex.getCause() instanceof SolrServerException) {
			SolrServerException solrServerException = (SolrServerException) ex.getCause();
			if (solrServerException.getCause() instanceof SolrException) {
				SolrException solrException = (SolrException) solrServerException.getCause();
				// this will fail with solr 4.0.x as ParseExecption moved to org.apache.lucene.queryparser.classic
				if (solrException.getCause() instanceof ParseException) {
					return new InvalidDataAccessApiUsageException(((ParseException) solrException.getCause()).getMessage(),
							solrException.getCause());
				} else {
					ErrorCode errorCode = SolrException.ErrorCode.getErrorCode(solrException.code());
					switch (errorCode) {
					case NOT_FOUND:
					case SERVICE_UNAVAILABLE:
					case SERVER_ERROR:
						return new DataAccessResourceFailureException(solrException.getMessage(), solrException);
					case FORBIDDEN:
					case UNAUTHORIZED:
						return new PermissionDeniedDataAccessException(solrException.getMessage(), solrException);
					case BAD_REQUEST:
						return new InvalidDataAccessApiUsageException(solrException.getMessage(), solrException);
					case UNKNOWN:
						return new UncategorizedSolrException(solrException.getMessage(), solrException);
					default:
						break;
					}
				}

			}
		}
		return null;
	}
}