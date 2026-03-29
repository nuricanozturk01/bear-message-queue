import {
  AbstractMermaidTokenBuilder,
  CommonValueConverter,
  EmptyFileSystem,
  GitGraphGeneratedModule,
  MermaidGeneratedSharedModule,
  __name,
  createDefaultCoreModule,
  createDefaultSharedCoreModule,
  inject,
  lib_exports
} from "./chunk-ISZED5UE.js";

// node_modules/@mermaid-js/parser/dist/chunks/mermaid-parser.core/chunk-NCMFTTUW.mjs
var GitGraphTokenBuilder = class extends AbstractMermaidTokenBuilder {
  static {
    __name(this, "GitGraphTokenBuilder");
  }
  constructor() {
    super(["gitGraph"]);
  }
};
var GitGraphModule = {
  parser: {
    TokenBuilder: __name(() => new GitGraphTokenBuilder(), "TokenBuilder"),
    ValueConverter: __name(() => new CommonValueConverter(), "ValueConverter")
  }
};
function createGitGraphServices(context = EmptyFileSystem) {
  const shared = inject(
    createDefaultSharedCoreModule(context),
    MermaidGeneratedSharedModule
  );
  const GitGraph = inject(
    createDefaultCoreModule({ shared }),
    GitGraphGeneratedModule,
    GitGraphModule
  );
  shared.ServiceRegistry.register(GitGraph);
  return { shared, GitGraph };
}
__name(createGitGraphServices, "createGitGraphServices");

export {
  GitGraphModule,
  createGitGraphServices
};
//# sourceMappingURL=chunk-L5U72XPQ.js.map
