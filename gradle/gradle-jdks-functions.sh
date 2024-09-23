#!/bin/sh

set -e
# Set pipefail if it works in a subshell, disregard if unsupported
# shellcheck disable=SC3040
if (set -o  pipefail 2>/dev/null); then
    set -o pipefail
fi
#
# (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

TMP_WORK_DIR=$(mktemp -d)
export TMP_WORK_DIR

cleanup() {
  [ -d "$TMP_WORK_DIR" ] && rm -rf "$TMP_WORK_DIR"
}

die() {
    echo
    echo "$*"
    echo
    cleanup
    exit 1
} >&2

read_value() {
  if [ ! -f "$1" ]; then
    die "ERROR: $1 not found, aborting Gradle JDK setup"
  fi
  read -r value < "$1" || die "ERROR: Unable to read value from $1. Make sure the file ends with a newline."
  echo "$value"
}

get_os() {
  # OS specific support; same as gradle-jdks:com.palantir.gradle.jdks.setup.common.CurrentOs.java
  case "$( uname )" in                          #(
    Linux* )          os_name="linux"  ;;       #(
    Darwin* )         os_name="macos"  ;;       #(
    * )               die "ERROR Unsupported OS: $( uname )" ;;
  esac

  if [ "$os_name" = "linux" ]; then
      ldd_output=$(ldd --version 2>&1 || true)
      if echo "$ldd_output" | grep -qi glibc; then
        os_name="linux-glibc"
      elif echo "$ldd_output" | grep -qi "gnu libc"; then
        os_name="linux-glibc"
      elif echo "$ldd_output" | grep -qi musl; then
        os_name="linux-musl"
      else
        die "Unable to determine glibc or musl based Linux distribution: ldd_output: $ldd_output"
      fi
  fi

  echo "$os_name"
}

get_arch() {
  # Arch specific support, see: gradle-jdks:com.palantir.gradle.jdks.setup.common.CurrentArch.java
  case "$(uname -m)" in                         #(
    x86_64* )       arch_name="x86-64"  ;;      #(
    x64* )          arch_name="x86-64"  ;;      #(
    amd64* )        arch_name="x86-64"  ;;      #(
    arm64* )        arch_name="aarch64"  ;;     #(
    arm* )          arch_name="aarch64"  ;;     #(
    aarch64* )      arch_name="aarch64"  ;;     #(
    x86* )          arch_name="x86"  ;;         #(
    i686* )         arch_name="x86"  ;;         #(
    * )             die "ERROR Unsupported architecture: $( uname -m )" ;;
  esac

  echo "$arch_name"
}

get_gradle_jdks_home() {
  gradle_user_home=${GRADLE_USER_HOME:-"$HOME"/.gradle}
  gradle_jdks_home="$gradle_user_home"/gradle-jdks
  echo "$gradle_jdks_home"
}

get_java_home() {
  java_bin=$(find "$1" -type f -name "java" -path "*/bin/java" ! -type l -print -quit)
  echo "${java_bin%/*/*}"
}

GRADLE_JDKS_HOME=$(get_gradle_jdks_home)
mkdir -p "$GRADLE_JDKS_HOME"
export GRADLE_JDKS_HOME

OS=$(get_os)
export OS

ARCH=$(get_arch)
export ARCH

install_and_setup_jdks() {
  gradle_dir=$1
  scripts_dir=${2:-"$1"}

  for dir in "$gradle_dir"/jdks/*/; do
    major_version_dir=${dir%*/}
    major_version=${major_version_dir##*/}
    if [ "$major_version" = "8" ]; then
      echo "Skipping JDK 8 installation as it is not supported by Gradle JDKs Setup."
      continue
    fi
    distribution_local_path=$(read_value "$major_version_dir"/"$OS"/"$ARCH"/local-path)
    distribution_url=$(read_value "$major_version_dir"/"$OS"/"$ARCH"/download-url)
    # Check if distribution exists in $GRADLE_JDKS_HOME
    jdk_installation_directory="$GRADLE_JDKS_HOME"/"$distribution_local_path"
    if [ ! -d "$jdk_installation_directory" ]; then
      # Download and extract the distribution into a temporary directory
      echo "JDK installation '$jdk_installation_directory' does not exist, installing '$distribution_url' in progress ..."
      in_progress_dir="$TMP_WORK_DIR/$distribution_local_path.in-progress"
      mkdir -p "$in_progress_dir"
      cd "$in_progress_dir" || die "failed to change dir to $in_progress_dir"
      if command -v curl > /dev/null 2>&1; then
        echo "Using curl to download $distribution_url"
        case "$distribution_url" in
          *.zip)
            distribution_name=${distribution_url##*/}
            curl -C - "$distribution_url" -o "$distribution_name"
            tar -xzf "$distribution_name"
            ;;
          *)
            curl -C - "$distribution_url" | tar -xzf -
            ;;
        esac
      elif command -v wget > /dev/null 2>&1; then
        echo "Using wget to download $distribution_url"
        case "$distribution_url" in
          *.zip)
            distribution_name=${distribution_url##*/}
            wget -c "$distribution_url" -O "$distribution_name"
            tar -xzf "$distribution_name"
            ;;
          *)
            wget -qO- -c "$distribution_url" | tar -xzf -
            ;;
        esac
      else
        die "ERROR: Neither curl nor wget are installed, Could not set up JAVA_HOME"
      fi
      cd - || exit

      # Finding the java_home
      java_home=$(get_java_home "$in_progress_dir")
      "$java_home"/bin/java -cp "$scripts_dir"/gradle-jdks-setup.jar com.palantir.gradle.jdks.setup.GradleJdkInstallationSetup jdkSetup "$jdk_installation_directory" || die "Failed to set up JDK $jdk_installation_directory"
      echo "Successfully installed JDK distribution in $jdk_installation_directory"
    fi
  done
}
