# Java Command Line Interface (CLI) 🖥️📂

A Java-based simulation of a Unix-like terminal environment, supporting common shell commands with features like piping (`|`), redirection (`>` and `>>`), file/directory manipulation, and robust unit testing using JUnit.

## 📘 Overview

This project provides a text-based Command Line Interface (CLI) developed in Java.  
It allows users to interact with the file system using familiar shell-like commands.

**Key Features:**
- Support for common shell commands (`cd`, `ls`, `mkdir`, `touch`, `rm`, `cat`, etc.)
- `|` (pipe) operator support for chaining commands
- Output redirection (`>` and `>>`)
- Thread-safe design
- Fully testable with JUnit (80+% coverage)

## 🧠 Concepts Covered

- Java File I/O (NIO)
- Command Parsing
- Exception Handling
- Standard Shell Behaviors
- Unit Testing (JUnit 5)
- System Input/Output Redirection

## 💡 Sample Commands

```sh
pwd                             # Print current working directory
cd folderName                   # Change directory
ls                              # List files
ls -a                           # List all (including hidden)
ls -r                           # List in reverse order
mkdir myFolder                  # Create new directory
touch file.txt                  # Create empty file
rm file.txt                     # Delete file
rmdir folderName                # Delete empty directory
mv old.txt new.txt              # Rename or move file
cat file.txt                    # Display file content
cat > file.txt                  # Write to file (until 'Exit' is typed)
command > out.txt               # Redirect output to file (overwrite)
command >> out.txt              # Redirect output to file (append)
command1 | command2             # Pipe output of command1 into command2
