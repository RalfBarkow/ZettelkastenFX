{
  description = "Dev shell for ZettelkastenFX (JDK17 + Maven + JavaFX)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        jdk = pkgs.jdk17;   # LTS, matches pom's source/target=17
        maven = pkgs.maven;

        # Native libs for JavaFX on Linux
        linuxLibs = with pkgs; [
          gtk3 glib
          libX11 libXi libXext libXrandr libXrender libXtst
          alsaLib freetype fontconfig
          mesa libglvnd zlib
        ];
      in {
        devShells.default = pkgs.mkShell {
          packages = [ jdk maven ] ++ pkgs.lib.optionals pkgs.stdenv.isLinux linuxLibs;

          shellHook = ''
            export JAVA_HOME=${jdk}
            # Append headless=false to MAVEN_OPTS (avoid shell parameter tricks that clash with Nix interpolation)
            export MAVEN_OPTS="$MAVEN_OPTS -Djava.awt.headless=false"

            ${pkgs.lib.optionalString pkgs.stdenv.isLinux ''
              export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath linuxLibs}:$LD_LIBRARY_PATH
              export GTK_PATH=${pkgs.gtk3}/lib/gtk-3.0
            ''}

            echo "▶ ZettelkastenFX dev shell"
            echo "   Java:  $(java -version 2>&1 | head -n1)"
            echo "   Maven: $(mvn -v | head -n1)"
            echo
            echo "Run the JavaFX app:"
            echo "  mvn -q -pl zk-ui-javafx -am javafx:run"
          '';
        };
      }
    );
}
