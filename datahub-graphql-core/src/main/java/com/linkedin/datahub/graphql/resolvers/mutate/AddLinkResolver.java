package com.linkedin.datahub.graphql.resolvers.mutate;

import com.linkedin.common.urn.CorpuserUrn;

import com.linkedin.common.urn.Urn;
import com.linkedin.datahub.graphql.QueryContext;
import com.linkedin.datahub.graphql.exception.AuthorizationException;
import com.linkedin.datahub.graphql.generated.AddLinkInput;
import com.linkedin.datahub.graphql.resolvers.mutate.util.LinkUtils;
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
public class AddLinkResolver implements DataFetcher<CompletableFuture<Boolean>> {

  private final EntityService _entityService;

  @Override
  public CompletableFuture<Boolean> get(DataFetchingEnvironment environment) throws Exception {
    final String condUpdate = environment.getVariables().containsKey(Constants.IN_UNMODIFIED_SINCE)
            ? environment.getVariables().get(Constants.IN_UNMODIFIED_SINCE).toString() : null;
    final AddLinkInput input = bindArgument(environment.getArgument("input"), AddLinkInput.class);

    String linkUrl = input.getLinkUrl();
    String linkLabel = input.getLabel();
    Urn targetUrn = Urn.createFromString(input.getResourceUrn());

    if (!LinkUtils.isAuthorizedToUpdateLinks(environment.getContext(), targetUrn)) {
      throw new AuthorizationException("Unauthorized to perform this action. Please contact your DataHub administrator.");
    }

    return CompletableFuture.supplyAsync(() -> {
      LinkUtils.validateAddRemoveInput(
          linkUrl,
          targetUrn,
          _entityService
      );
      try {

        log.debug("Adding Link. input: {}", input);

        Urn actor = CorpuserUrn.createFromString(((QueryContext) environment.getContext()).getActorUrn());
        LinkUtils.addLink(
            linkUrl,
            linkLabel,
            targetUrn,
            actor,
            _entityService,
            condUpdate
        );
        return true;
      } catch (Exception e) {
        log.error("Failed to add link to resource with input {}, {}", input, e.getMessage());
        throw new RuntimeException(String.format("Failed to add link to resource with input %s", input), e);
      }
    });
  }
}
