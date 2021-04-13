package com.linkedin.gms.factory.tag;

import com.linkedin.gms.factory.common.IndexConventionFactory;
import com.linkedin.metadata.configs.TagSearchConfig;
import com.linkedin.metadata.dao.search.ESSearchDAO;
import com.linkedin.metadata.search.TagDocument;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.Nonnull;


@Configuration
public class TagSearchDaoFactory {
  @Autowired
  ApplicationContext applicationContext;

  @Bean(name = "tagSearchDAO")
  @DependsOn({"elasticSearchRestHighLevelClient", IndexConventionFactory.INDEX_CONVENTION_BEAN})
  @Nonnull
  protected ESSearchDAO createInstance() {
    return new ESSearchDAO(applicationContext.getBean(RestHighLevelClient.class), TagDocument.class,
        new TagSearchConfig(applicationContext.getBean(IndexConvention.class)));
  }
}
