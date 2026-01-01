{
  description = "Android-Nix";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    utils.url = "github:numtide/flake-utils";
  };

  outputs =
    { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
        };
      in
      with pkgs;
      rec {
        formatter = pkgs.nixfmt-tree;
        devShells.default = mkShell {
          packages = with pkgs; [
            android-tools
            fastlane
            imagemagick
            ktlint
            detekt
          ];
        };
      }
    );
}
