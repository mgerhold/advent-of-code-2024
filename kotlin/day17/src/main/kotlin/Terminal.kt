package org.example

class Terminal {
    companion object {
        private const val ESC = "\u001B"

        fun clearScreen() {
            print("${ESC}[2J${ESC}[H")
        }

        fun setCursorPosition(row: Int, col: Int) {
            print("${ESC}[${row};${col}H")
        }

        fun hideCursor() {
            print("${ESC}[?25l")
        }

        fun showCursor() {
            print("${ESC}[?25h")
        }

        fun setForegroundColor(color: AnsiColor) {
            print("${ESC}[${color.code}m")
        }

        fun resetAttributes() {
            print("${ESC}[0m")
        }
    }
}
