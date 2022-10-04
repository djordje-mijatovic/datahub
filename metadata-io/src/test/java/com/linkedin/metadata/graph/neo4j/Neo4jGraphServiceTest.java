package com.linkedin.metadata.graph.neo4j;

import com.linkedin.common.urn.Urn;

import com.linkedin.metadata.graph.EntityLineageResult;
import com.linkedin.metadata.graph.GraphService;
import com.linkedin.metadata.graph.GraphServiceTestBase;
import com.linkedin.metadata.graph.LineageDirection;
import com.linkedin.metadata.graph.LineageRelationship;
import com.linkedin.metadata.graph.RelatedEntitiesResult;
import com.linkedin.metadata.graph.RelatedEntity;
import com.linkedin.metadata.models.registry.LineageRegistry;
import com.linkedin.metadata.models.registry.SnapshotEntityRegistry;
import com.linkedin.metadata.query.filter.RelationshipFilter;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class Neo4jGraphServiceTest extends GraphServiceTestBase {

  private Neo4jTestServerBuilder _serverBuilder;
  private Driver _driver;
  private Neo4jGraphService _client;

  @BeforeMethod
  public void init() {
    _serverBuilder = new Neo4jTestServerBuilder();
    _serverBuilder.newServer();
    _driver = GraphDatabase.driver(_serverBuilder.boltURI());
    _client = new Neo4jGraphService(new LineageRegistry(SnapshotEntityRegistry.getInstance()), _driver);
  }

  @AfterMethod
  public void tearDown() {
    _serverBuilder.shutdown();
  }

  @Override
  protected @Nonnull
  GraphService getGraphService() {
    return _client;
  }

  @Override
  protected void syncAfterWrite() {
  }

  @Override
  protected void assertEqualsAnyOrder(RelatedEntitiesResult actual, RelatedEntitiesResult expected) {
    // https://github.com/datahub-project/datahub/issues/3118
    // Neo4jGraphService produces duplicates, which is here ignored until fixed
    // actual.count and actual.total not tested due to duplicates
    assertEquals(actual.getStart(), expected.getStart());
    assertEqualsAnyOrder(actual.getEntities(), expected.getEntities(), RELATED_ENTITY_COMPARATOR);
  }

  @Override
  protected <T> void assertEqualsAnyOrder(List<T> actual, List<T> expected, Comparator<T> comparator) {
    // https://github.com/datahub-project/datahub/issues/3118
    // Neo4jGraphService produces duplicates, which is here ignored until fixed
    assertEquals(
            new HashSet<>(actual),
            new HashSet<>(expected)
    );
  }

  @Override
  public void testFindRelatedEntitiesSourceType(String datasetType,
                                                List<String> relationshipTypes,
                                                RelationshipFilter relationships,
                                                List<RelatedEntity> expectedRelatedEntities) throws Exception {
    if (datasetType != null && datasetType.isEmpty()) {
      // https://github.com/datahub-project/datahub/issues/3119
      throw new SkipException("Neo4jGraphService does not support empty source type");
    }
    if (datasetType != null && datasetType.equals(GraphServiceTestBase.userType)) {
      // https://github.com/datahub-project/datahub/issues/3123
      // only test cases with "user" type fail due to this bug
      throw new SkipException("Neo4jGraphService does not apply source / destination types");
    }
    super.testFindRelatedEntitiesSourceType(datasetType, relationshipTypes, relationships, expectedRelatedEntities);
  }

  @Override
  public void testFindRelatedEntitiesDestinationType(String datasetType,
                                                     List<String> relationshipTypes,
                                                     RelationshipFilter relationships,
                                                     List<RelatedEntity> expectedRelatedEntities) throws Exception {
    if (datasetType != null && datasetType.isEmpty()) {
      // https://github.com/datahub-project/datahub/issues/3119
      throw new SkipException("Neo4jGraphService does not support empty destination type");
    }
    if (relationshipTypes.contains(hasOwner)) {
      // https://github.com/datahub-project/datahub/issues/3123
      // only test cases with "HasOwner" relatioship fail due to this bug
      throw new SkipException("Neo4jGraphService does not apply source / destination types");
    }
    super.testFindRelatedEntitiesDestinationType(datasetType, relationshipTypes, relationships, expectedRelatedEntities);
  }

  @Test
  @Override
  public void testFindRelatedEntitiesNullSourceType() throws Exception {
    // https://github.com/datahub-project/datahub/issues/3121
    throw new SkipException("Neo4jGraphService does not support 'null' entity type string");
  }

  @Test
  @Override
  public void testFindRelatedEntitiesNullDestinationType() throws Exception {
    // https://github.com/datahub-project/datahub/issues/3121
    throw new SkipException("Neo4jGraphService does not support 'null' entity type string");
  }

  @Test
  @Override
  public void testFindRelatedEntitiesNoRelationshipTypes() {
    // https://github.com/datahub-project/datahub/issues/3120
    throw new SkipException("Neo4jGraphService does not support empty list of relationship types");
  }

  @Test
  @Override
  public void testRemoveEdgesFromNodeNoRelationshipTypes() {
    // https://github.com/datahub-project/datahub/issues/3120
    throw new SkipException("Neo4jGraphService does not support empty list of relationship types");
  }

  @Test
  @Override
  public void testConcurrentAddEdge() {
    // https://github.com/datahub-project/datahub/issues/3141
    throw new SkipException("Neo4jGraphService does not manage to add all edges added concurrently");
  }

  @Test
  @Override
  public void testConcurrentRemoveEdgesFromNode() {
    // https://github.com/datahub-project/datahub/issues/3118
    throw new SkipException("Neo4jGraphService produces duplicates");
  }

  @Test
  @Override
  public void testConcurrentRemoveNodes() {
    // https://github.com/datahub-project/datahub/issues/3118
    throw new SkipException("Neo4jGraphService produces duplicates");
  }

  @Test
  public void testPopulatedGraphServiceGetLineageMultihop() throws Exception {
    GraphService service = getLineagePopulatedGraphService();

    EntityLineageResult upstreamLineage = service.getLineage(datasetOneUrn, LineageDirection.UPSTREAM, 0, 1000, 2);
    assertEquals(upstreamLineage.getTotal().intValue(), 0);
    assertEquals(upstreamLineage.getRelationships().size(), 0);

    EntityLineageResult downstreamLineage = service.getLineage(datasetOneUrn, LineageDirection.DOWNSTREAM, 0, 1000, 2);

    assertEquals(downstreamLineage.getTotal().intValue(), 5);
    assertEquals(downstreamLineage.getRelationships().size(), 5);
    Map<Urn, LineageRelationship> relationships = downstreamLineage.getRelationships().stream().collect(Collectors.toMap(LineageRelationship::getEntity,
            Function.identity()));
    assertTrue(relationships.containsKey(datasetTwoUrn));
    assertEquals(relationships.get(datasetTwoUrn).getDegree().intValue(), 1);
    assertTrue(relationships.containsKey(datasetThreeUrn));
    assertEquals(relationships.get(datasetThreeUrn).getDegree().intValue(), 2);
    assertTrue(relationships.containsKey(datasetFourUrn));
    assertEquals(relationships.get(datasetFourUrn).getDegree().intValue(), 2);
    assertTrue(relationships.containsKey(dataJobOneUrn));
    assertEquals(relationships.get(dataJobOneUrn).getDegree().intValue(), 1);
    assertTrue(relationships.containsKey(dataJobTwoUrn));
    assertEquals(relationships.get(dataJobTwoUrn).getDegree().intValue(), 1);

    upstreamLineage = service.getLineage(datasetThreeUrn, LineageDirection.UPSTREAM, 0, 1000, 2);
    assertEquals(upstreamLineage.getTotal().intValue(), 3);
    assertEquals(upstreamLineage.getRelationships().size(), 3);
    relationships = upstreamLineage.getRelationships().stream().collect(Collectors.toMap(LineageRelationship::getEntity,
            Function.identity()));
    assertTrue(relationships.containsKey(datasetOneUrn));
    assertEquals(relationships.get(datasetOneUrn).getDegree().intValue(), 2);
    assertTrue(relationships.containsKey(datasetTwoUrn));
    assertEquals(relationships.get(datasetTwoUrn).getDegree().intValue(), 1);
    assertTrue(relationships.containsKey(dataJobOneUrn));
    assertEquals(relationships.get(dataJobOneUrn).getDegree().intValue(), 1);

    downstreamLineage = service.getLineage(datasetThreeUrn, LineageDirection.DOWNSTREAM, 0, 1000, 2);
    assertEquals(downstreamLineage.getTotal().intValue(), 0);
    assertEquals(downstreamLineage.getRelationships().size(), 0);
  }
}
