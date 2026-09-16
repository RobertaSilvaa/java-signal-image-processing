# Signals Project

Java project for one-dimensional and two-dimensional convolution experiments.

## Project Structure

```text
java-signal-image-processing/
├── .gitignore
├── README.md
├── .vscode/
│   └── settings.json
└── src/
    ├── Convolution1D.java
    ├── Convolution2D.java
    └── image/
        └── Cat.jpg
```

## Requirements

- Java Development Kit (JDK) 8 or newer
- A graphical desktop environment for the Swing user interfaces

Check the installed Java version with:

```bash
java -version
javac -version
```

## Compilation

Run the commands from the project root.

### Windows PowerShell

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -d bin src\Convolution1D.java src\Convolution2D.java
```

### Linux or macOS

```bash
mkdir -p bin
javac -d bin src/Convolution1D.java src/Convolution2D.java
```

## Running the 1D Convolution Program

```bash
java -cp bin Convolution1D
```

The application displays the original signal, noisy signal, impulse response, and convolution result.

## Running the 2D Convolution Program

```bash
java -cp bin Convolution2D
```

By default, the program automatically checks the image location according to the current working directory:

```text
src/image/Cat.jpg
image/Cat.jpg
```

This allows the program to work both when it is started from the project root and when VS Code Code Runner starts it from inside the `src` directory.

You may also provide another image path as a command-line argument:

```bash
java -cp bin Convolution2D "path/to/image.jpg"
```

Example on Windows PowerShell:

```powershell
java -cp bin Convolution2D "C:\Users\User\Pictures\example.jpg"
```


### Running with VS Code Code Runner

If Code Runner executes `Convolution2D.java` directly from the `src` directory, a command similar to the following is expected:

```powershell
cd src
javac Convolution2D.java
java Convolution2D
```

In this case, the default image is resolved as:

```text
image/Cat.jpg
```

The generated `.class` files are ignored by Git through `.gitignore`.

## Implemented Operations

### 1D

The 1D application:

- generates a discrete signal;
- adds noise to the signal;
- generates an impulse response controlled by coefficient `a`;
- computes the discrete convolution;
- displays the signals graphically;
- shows the complete convolution result in a table.

### 2D

The 2D application applies convolution-based image filters, including:

- smoothing;
- edge detection;
- enhancement;
- sharpening;
- Gaussian filtering.

The program validates image loading, matrix dimensions, kernel dimensions, and invalid numerical values before processing.
