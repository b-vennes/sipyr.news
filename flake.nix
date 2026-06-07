{
    description = "Projects";

    inputs.flake-utils.url = "github:numtide/flake-utils";

    outputs = { self, nixpkgs, flake-utils }:
        flake-utils.lib.eachDefaultSystem (system:
            let pkgs = nixpkgs.legacyPackages.${system}; in
            with pkgs; {
                devShells.default = mkShell {
                    packages = [
                        zsh
                        starship
                        neovim
                        git
                        github-cli
                        mill
                        deno
                    ];
                };
            }
        );
}