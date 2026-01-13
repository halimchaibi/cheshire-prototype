#!/usr/bin/env bash

MODE=${1:-"structure"}
DIR=${2:-"."}

if [[ ! -d "$DIR" ]]; then
    echo "Usage: $0 [-c|-cc|-cx|-ccx] <directory>"
    exit 1
fi

# Helper to format the file header
print_file_header() {
    echo "------------------------------------------------"
    echo "FILE: $1"
    echo "------------------------------------------------"
}

extract_skeleton() {
    local file="$1"
    local show_cmts="$2"

    awk -v show="$show_cmts" '
    BEGIN { in_cmt = 0 }
    
    # Block Comment Toggle
    /\/\*/ { in_cmt = 1; if(show=="1") print $0; next }
    in_cmt { if(show=="1") print $0; if(/\*\//) in_cmt = 0; next }
    
    # Single line comments
    /^\s*\/\// { if(show=="1") print $0; next }

    {
        line = $0
        # 1. Capture Package/Imports
        if (line ~ /^\s*(package|import)\s+/) {
            print line
        }
        # 2. Capture Class/Interface/Enum
        else if (line ~ /^\s*(public|private|protected|abstract|static )*(class|interface|enum|record)\s+\w+/) {
            sub(/\{.*/, " {", line)
            print "\n" line
        }
        # 3. Capture Method Signatures 
        # Logic: Must have "(" and ")" and NOT end in ";" (to ignore fields/abstract)
        else if (line ~ /\(.*\)/ && line !~ /;/) {
            sub(/\{.*/, "", line) # Strip start of body
            gsub(/^\s+/, "", line) # Clean indentation
            print "    " line " {}"
        }
        # 4. Capture Annotations
        else if (line ~ /^\s*@/) {
            print "    " line
        }
    }' "$file"
}

case "$MODE" in
    "-c")
        find "$DIR" -name "*.java" | sort | while read -r file; do
            print_file_header "$file"
            extract_skeleton "$file" "0"
            echo -e "\n"
        done
        ;;
    "-cx")
        find "$DIR" -name "*.java" | sort | while read -r file; do
            print_file_header "$file"
            extract_skeleton "$file" "1"
            echo -e "\n"
        done
        ;;
    "-cc")
        find "$DIR" -name "*.java" | sort | while read -r file; do
            print_file_header "$file"
            sed 's/\/\/.*//' "$file" | awk '/\/\*/{p=1} !p; /\*\//{p=0}' | sed '/^\s*$/d'
        done
        ;;
    "-ccx")
        find "$DIR" -name "*.java" | sort | while read -r file; do
            print_file_header "$file"
            cat "$file"
        done
        ;;
    *)
        # Default: Just show the file list if no valid flag is passed
        find "$DIR" -name "*.java" | sed "s|^$DIR/||" | sort
        ;;
esac