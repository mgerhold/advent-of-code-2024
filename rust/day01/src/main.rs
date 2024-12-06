use std::collections::HashMap;

fn main() {
    let input = std::fs::read_to_string("input.txt").unwrap();
    let (mut left, mut right): (Vec<_>, Vec<_>) = input
        .lines()
        .map(|line| {
            line.split("   ")
                .map(|x| x.parse::<i32>().unwrap())
                .collect::<Vec<_>>()
        })
        .map(|numbers| (numbers[0], numbers[1]))
        .unzip();

    // Part 1.
    left.sort();
    right.sort();
    let result: i32 = left
        .iter()
        .zip(right.iter())
        .map(|(x, y)| (x - y).abs())
        .sum();
    println!("Result: {}", result);

    // Part 2.
    let mut counts = HashMap::new();
    for number in right {
        *counts.entry(number).or_insert(0) += 1;
    }
    let result: i32 = left
        .iter()
        .map(|number| number * counts.get(&number).unwrap_or(&0))
        .sum();
    println!("Result: {}", result);
}
