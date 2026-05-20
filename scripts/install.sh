#!/usr/bin/env sh
set -eu

install_dir="${INVOICE2MD_INSTALL_DIR:-$HOME/.local/share/invoice2md}"
bin_dir="${INVOICE2MD_BIN_DIR:-$HOME/.local/bin}"

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
version=$(tr -d '[:space:]' < "$repo_root/VERSION")
jar_name="invoice2md-$version-standalone.jar"
built_jar="$repo_root/target/$jar_name"
installed_jar="$install_dir/invoice2md-standalone.jar"
launcher="$bin_dir/invoice2md"

cd "$repo_root"
clojure -T:build uber

mkdir -p "$install_dir" "$bin_dir"
cp "$built_jar" "$installed_jar"

cat > "$launcher" <<EOF
#!/usr/bin/env sh
exec java -jar "$installed_jar" "\$@"
EOF
chmod +x "$launcher"

echo "Installed invoice2md to $install_dir"
echo "Launcher: $launcher"

case ":$PATH:" in
  *":$bin_dir:"*) ;;
  *) echo "Add $bin_dir to PATH, then run: invoice2md convert ..." ;;
esac
