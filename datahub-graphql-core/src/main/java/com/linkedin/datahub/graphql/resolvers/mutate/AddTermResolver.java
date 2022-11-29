package com.linkedin.datahub.graphql.resolvers.mutate;

import com.google.common.collect.ImmutableList;
import com.linkedin.common.urn.CorpuserUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.datahub.graphql.QueryContext;
import com.linkedin.datahub.graphql.exception.AuthorizationException;
import com.linkedin.datahub.graphql.generated.ResourceRefInput;
import com.linkedin.datahub.graphql.generated.TermAssociationInput;
import com.linkedin.datahub.graphql.resolvers.mutate.util.LabelUtils;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.entity.EntityService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.linkedin.datahub.graphql.resolvers.ResolverUtils.*;

@Slf4j
@RequiredArgsConstructor
public class AddTermResolver implements DataFetcher<CompletableFuture<Boolean>> {
  private final EntityService _entityService;

  @Override
  public CompletableFuture<Boolean> get(DataFetchingEnvironment environment) throws Exception {
    final String condUpdate = environment.getVariables().containsKey(Constants.IN_UNMODIFIED_SINCE)
            ? environment.getVariables().get(Constants.IN_UNMODIFIED_SINCE).toString() : null;
    final TermAssociationInput input = bindArgument(environment.getArgument("input"), TermAssociationInput.class);
    Urn termUrn = Urn.createFromString(input.getTermUrn());
    Urn targetUrn = Urn.createFromString(input.getResourceUrn());

    if (!LabelUtils.isAuthorizedToUpdateTerms(environment.getContext(), targetUrn, input.getSubResource())) {
      throw new AuthorizationException("Unauthorized to perform this action. Please contact your DataHub administrator.");
    }

    return CompletableFuture.supplyAsync(() -> {
      LabelUtils.validateResourceAndLabel(
          termUrn,
          targetUrn,
          input.getSubResource(),
          input.getSubResourceType(),
          Constants.GLOSSARY_TERM_ENTITY_NAME,
          _entityService,
          false
      );

      try {
        log.info("Adding Term. input: {}", input);
        Urn actor = CorpuserUrn.createFromString(((QueryContext) environment.getContext()).getActorUrn());
        LabelUtils.addTermsToResources(
            ImmutableList.of(termUrn),
            ImmutableList.of(new ResourceRefInput(input.getResourceUrn(), input.getSubResourceType(), input.getSubResource())),
            actor,
            _entityService,
            condUpdate
        );
        return true;
      } catch (Exception e) {
        log.error("Failed to perform update against input {}, {}", input, e.getMessage());
        throw new RuntimeException(String.format("Failed to perform update against input %s", input), e);
      }
    });
  }
}
