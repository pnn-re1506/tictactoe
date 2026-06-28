# TicTacToe Client-Server Refactor Report

## 1. Muc tieu

Project TicTacToe ban dau chay theo kieu terminal-based trong mot process duy nhat. Refactor nay chuyen project sang mo hinh:

- Client-server
- Single-user
- Single-threaded
- Terminal-based
- Server song lien tuc, khong restart sau moi van
- Server khong luu board state
- Client giu board state
- Human luon di truoc voi mark `1`
- Computer di sau voi mark `2`
- Port hardcode la `12345`
- Tat ca file Java nam chung package hien tai:

```java
package vgu.pe2026.ttt.basis;
```

## 2. Kien truc sau refactor

Project hien co cac file Java chinh:

```text
Board.java
Client.java
ComputerPlayer.java
HumanPlayer.java
Server.java
```

Vai tro tung file:

- `Board.java`: quan ly board, rule co ban, serialize va deserialize board.
- `Client.java`: chay terminal client, giu board state, gui request len server.
- `Server.java`: chay socket server, xu ly request, tinh nuoc di cua computer.
- `HumanPlayer.java`: doc input cua human tu terminal.
- `ComputerPlayer.java`: chon nuoc di cho computer.

## 3. File da thay doi

### `Board.java`

Giu lai cac method co san:

- `place(...)`
- `isEmpty(...)`
- `hasWon(...)`
- `isFull(...)`
- `firstEmptyCell(...)`
- `printMatrix()`

Them cac constant:

```java
public static final int EMPTY = 0;
public static final int HUMAN = 1;
public static final int COMPUTER = 2;
```

Them method validate move:

```java
public boolean isValidMove(int cell)
```

Them method chuyen board thanh chuoi de gui qua socket:

```java
public String toLine()
```

Vi du:

```text
Board:
1 0 0
0 2 0
0 0 0

String:
100020000
```

Them method tao board tu chuoi server/client gui:

```java
public static Board fromLine(String line)
```

Method nay validate:

- Chuoi khong duoc `null`
- Chuoi phai co dung 9 ky tu
- Moi ky tu chi duoc la `0`, `1`, hoac `2`

### `ComputerPlayer.java`

Truoc refactor, `ComputerPlayer` ke thua `Player`.

Sau refactor, `ComputerPlayer` la class doc lap:

```java
public class ComputerPlayer {
    public int chooseCell(Board board) {
        return board.firstEmptyCell();
    }
}
```

Ly do:

- Khong can abstraction `Player` nua.
- Server chi can computer chon mot o trong.
- Code don gian hon va phu hop voi kien truc moi.

### `HumanPlayer.java`

`HumanPlayer` duoc giu lai de `Client` co the reuse logic doc input.

Sau refactor:

- Khong con `extends Player`
- Khong goi `System.exit(0)`
- Chi doc va validate input
- Neu user nhap `q`, tra ve `-1`

`Client` se xu ly viec gui `QUIT` cho server.

### `Client.java`

File moi dung de chay client.

Client:

- Ket noi den `localhost:12345`
- Gui `START` de xin vao game
- Neu server tra `WAIT`, client cho 2 giay roi retry
- Giu board state local bang `Board board`
- Dung `HumanPlayer` de doc move cua human
- Gui board va move len server bang request `MOVE`
- Nhan response tu server
- Cap nhat board local
- In ket qua ra terminal

### `Server.java`

File moi dung de chay server.

Server:

- Listen tren port `12345`
- Chay vong lap vo han `while (true)`
- Khong dung thread
- Moi connection xu ly mot request roi dong connection
- Khong luu board state
- Chi giu bien `busy` de dam bao single-user

Bien `busy` chi la lock cho game hien tai, khong phai board state.

### `pom.xml`

Manifest main class duoc doi tu:

```text
vgu.pe2026.ttt.basis.Main
```

sang:

```text
vgu.pe2026.ttt.basis.Client
```

## 4. File da xoa

Da xoa cac file khong con can trong kien truc moi:

```text
Main.java
GamePlay.java
Player.java
```

Ly do:

- `Main.java`: flow cu chay terminal game trong mot process khong con dung.
- `GamePlay.java`: tron game loop, turn handling va terminal output, khong phu hop voi client-server.
- `Player.java`: abstraction cu khong con can vi `HumanPlayer` va `ComputerPlayer` da doc lap.

## 5. Protocol client-server

Protocol dung raw socket va plain text.

Khong dung JSON de tranh them dependency.

### START

Client gui:

```text
START
```

Neu server ranh:

```text
OK
```

Neu server dang co game:

```text
WAIT
```

### MOVE

Client gui:

```text
MOVE <board> <humanMove>
```

Vi du:

```text
MOVE 100000000 5
```

Server tra:

```text
RESULT <status> <board> <computerMove>
```

Vi du:

```text
RESULT ongoing 100020000 5
```

### QUIT

Client gui:

```text
QUIT
```

Server tra:

```text
OK
```

Server set `busy = false`.

## 6. Status cua game

Server co the tra cac status:

```text
ongoing
win
lose
draw
invalid
```

Y nghia:

- `ongoing`: game tiep tuc
- `win`: human thang
- `lose`: computer thang
- `draw`: hoa
- `invalid`: request hoac move khong hop le

## 7. Flow hoat dong

### Server flow

```text
Start server
-> listen port 12345
-> accept connection
-> read request
-> handle START / MOVE / QUIT
-> send response
-> close connection
-> continue listening
```

### Client flow

```text
Start client
-> send START
-> if WAIT: sleep 2 seconds and retry
-> if OK: start game
-> print board
-> read human move
-> send MOVE board move
-> receive RESULT
-> update local board
-> if ongoing: continue
-> if win / lose / draw: print final board and result
```

## 8. Waiting behavior

Server chi cho mot client choi tai mot thoi diem.

Khi game dang chay:

- Client moi van connect duoc
- Server tra `WAIT`
- Client in:

```text
Server is busy. Waiting...
```

- Client sleep 2 giay roi gui lai `START`

Cach nay don gian va khong can thread.

## 9. Cach chay project

Tu thu muc Maven project:

```powershell
cd C:\Users\phucn\OneDrive\Documents\tictactoe\tttbasic
mvn clean compile
```

Terminal 1 chay server:

```powershell
java -cp target\classes vgu.pe2026.ttt.basis.Server
```

Terminal 2 chay client:

```powershell
java -cp target\classes vgu.pe2026.ttt.basis.Client
```

Terminal 3 co the chay client thu hai de test waiting:

```powershell
java -cp target\classes vgu.pe2026.ttt.basis.Client
```

## 10. Kiem tra build

Da chay lenh:

```powershell
mvn clean test
```

Ket qua:

```text
BUILD SUCCESS
```

## 11. Ket luan

Phien ban moi da tach duoc terminal game cu thanh client-server don gian.

Server chi xu ly request va tinh nuoc di cua computer. Board state nam o client va duoc gui len server trong moi request `MOVE`.

Thiet ke nay dap ung cac rang buoc:

- Human luon di truoc
- Single-user
- Single-threaded
- Server song lien tuc
- Server khong luu board state
- Code van giu chung mot package de don gian hoa project
