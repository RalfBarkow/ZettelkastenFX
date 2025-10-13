{
  description = "Dev shell for ZettelkastenFX (JDK17 + Maven + JavaFX + SQLite CLI + IntelliJ IDEA CE)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        jdk = pkgs.jdk17;        # matches pom (source/target=17)
        maven = pkgs.maven;
        sqliteCli = pkgs.sqlite;  # handy to inspect ~/.zettelkastenfx/zk.db

        # Extra native libs needed by JavaFX on Linux
        linuxLibs = with pkgs; [
          gtk3 glib
          libX11 libXi libXext libXrandr libXrender libXtst
          alsaLib freetype fontconfig
          mesa libglvnd zlib
        ];

        # macOS launcher: run the installed IntelliJ CE so it inherits the devShell env
        ideaMacWrapper = pkgs.writeShellScriptBin "idea-community" ''
          set -euo pipefail
          for CAND in \
            "/Applications/IntelliJ IDEA CE.app/Contents/MacOS/idea" \
            "$HOME/Applications/IntelliJ IDEA CE.app/Contents/MacOS/idea"
          do
            if [ -x "$CAND" ]; then exec "$CAND" "$@"; fi
          done
          if command -v mdfind >/dev/null 2>&1; then
            FOUND=$(mdfind 'kMDItemCFBundleIdentifier == "com.jetbrains.intellij.ce"' | head -n1)
            if [ -n "$FOUND" ]; then exec "$FOUND/Contents/MacOS/idea" "$@"; fi
          fi
          echo "IntelliJ IDEA CE app not found. Install it, then run: idea-community ."
          exit 1
        '';
      in {
        devShells.default = pkgs.mkShell {
          packages =
            [ jdk maven sqliteCli ]
            ++ pkgs.lib.optionals pkgs.stdenv.isLinux linuxLibs
            ++ (if pkgs.stdenv.isLinux then [ pkgs.jetbrains.idea-community ] else [ ideaMacWrapper ]);

          shellHook = ''
            export JAVA_HOME=${jdk}
            # Quiet sqlite-jdbc native access warning globally (runtime + tests)
            export MAVEN_OPTS="$MAVEN_OPTS --enable-native-access=ALL-UNNAMED -Djava.awt.headless=false"
            # If you want IntelliJ itself to use this JDK instead of JetBrains Runtime, uncomment:
            # export IDEA_JDK="$JAVA_HOME"

            ${pkgs.lib.optionalString pkgs.stdenv.isLinux ''
              export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath linuxLibs}:$LD_LIBRARY_PATH
              export GTK_PATH=${pkgs.gtk3}/lib/gtk-3.0
            ''}

            # Quality-of-life aliases
            alias fxrun='mvn -q -f zk-ui-javafx/pom.xml javafx:run'
            alias fxexec='mvn -q -f zk-ui-javafx/pom.xml exec:java'
            alias uibuild='mvn -q -pl zk-ui-javafx -am -DskipTests install'
            alias storagetest='mvn -q -pl zk-storage-sqlite test'

            echo "▶ ZettelkastenFX dev shell"
            echo "   Java:  $(java -version 2>&1 | head -n1)"
            echo "   Maven: $(mvn -v | head -n1)"
            echo
            echo "Useful commands:"
            echo "  fxrun        # run JavaFX app (module POM)"
            echo "  uibuild      # build UI module + deps"
            echo "  storagetest  # run SQLite storage tests"
            echo "  sqlite3 ~/.zettelkastenfx/zk.db  # inspect DB"
            echo
            echo "IDEA: run 'idea-community .'  (or: nix run .#idea-ce .)"
          '';
        };

        apps.idea-ce = {
          type = "app";
          program =
            if pkgs.stdenv.isLinux
            then "${pkgs.jetbrains.idea-community}/bin/idea-community"
            else "${ideaMacWrapper}/bin/idea-community";
        };
      }
    );
}
