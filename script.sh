#!/usr/bin/env bash
# update_ksp_version_catalog.sh
# Walks the tree, finds libs.versions.toml, and ensures Kotlin/KSP versions and KSP plugin alias are present.
# Usage: ./update_ksp_version_catalog.sh [START_DIR]
set -euo pipefail

START_DIR="${1:-.}"

# Desired values (edit here if you need different versions)
KOTLIN_VER='2.0.21'
KSP_VER='2.0.21-1.0.25'
KSP_PLUGIN_LINE='ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }'

changed=0
touched=0

while IFS= read -r -d '' file; do
  touched=$((touched+1))
  cp -n -- "$file" "$file.bak"

  tmp="$(mktemp)"
  awk \
    -v V_KOTLIN="$KOTLIN_VER" \
    -v V_KSP="$KSP_VER" \
    -v P_KSP="$KSP_PLUGIN_LINE" '
    BEGIN {
      in_versions = 0
      in_plugins  = 0
      seen_versions_section = 0
      seen_plugins_section  = 0
      seen_versions_kotlin  = 0
      seen_versions_ksp     = 0
      seen_plugins_ksp      = 0
    }
    function flush_versions_trail() {
      if (in_versions) {
        if (!seen_versions_kotlin) print "kotlin = \"" V_KOTLIN "\""
        if (!seen_versions_ksp)    print "ksp = \"" V_KSP "\""
        in_versions = 0
      }
    }
    function flush_plugins_trail() {
      if (in_plugins) {
        if (!seen_plugins_ksp) print P_KSP
        in_plugins = 0
      }
    }
    /^[[:space:]]*\[[^]]+\][[:space:]]*$/ {
      # Close previous sections before switching
      if (in_versions) flush_versions_trail()
      if (in_plugins)  flush_plugins_trail()

      hdr = $0
      # Detect target sections
      if ($0 ~ /^[[:space:]]*\[versions\][[:space:]]*$/) {
        in_versions = 1; in_plugins = 0; seen_versions_section = 1
      } else if ($0 ~ /^[[:space:]]*\[plugins\][[:space:]]*$/) {
        in_plugins  = 1; in_versions = 0; seen_plugins_section  = 1
      } else {
        in_versions = 0; in_plugins  = 0
      }
      print hdr
      next
    }

    {
      if (in_versions) {
        if ($0 ~ /^[[:space:]]*kotlin[[:space:]]*=/) {
          print "kotlin = \"" V_KOTLIN "\""
          seen_versions_kotlin = 1
          next
        }
        if ($0 ~ /^[[:space:]]*ksp[[:space:]]*=/) {
          print "ksp = \"" V_KSP "\""
          seen_versions_ksp = 1
          next
        }
      }
      if (in_plugins) {
        if ($0 ~ /^[[:space:]]*ksp[[:space:]]*=/) {
          print P_KSP
          seen_plugins_ksp = 1
          next
        }
      }
      print $0
    }

    END {
      # If file ended inside a tracked section, flush missing keys
      if (in_versions) flush_versions_trail()
      if (in_plugins)  flush_plugins_trail()

      # If sections never existed, append them at EOF with required keys
      if (!seen_versions_section) {
        print ""
        print "[versions]"
        print "kotlin = \"" V_KOTLIN "\""
        print "ksp = \"" V_KSP "\""
      }
      if (!seen_plugins_section) {
        print ""
        print "[plugins]"
        print P_KSP
      }
    }
  ' "$file" > "$tmp"

  if ! cmp -s "$file" "$tmp"; then
    mv -- "$tmp" "$file"
    echo "Updated: $file"
    changed=$((changed+1))
  else
    rm -f -- "$tmp"
    echo "No change: $file"
  fi
done < <(find "$START_DIR" -type f -name 'libs.versions.toml' -print0)

echo "Done. Catalogs scanned: $touched; updated: $changed."
echo "Backups saved as *.bak next to each modified file."

