# Witness test cases

Some of the example witnesses of the sv-witnesses repository, with the programs they refer to,
copied from `examples/` at commit `0ef19deb3181b0082a5b2628872960e32a5d1a87`:
https://gitlab.com/sosy-lab/benchmarking/sv-witnesses/-/tree/0ef19deb3181b0082a5b2628872960e32a5d1a87/examples

- `java/`: `ExDarko_false.witness-2.4.yml` (violation) and `ExDarko_true.witness-2.4.yml` (correctness)
- `zero_div.witness-2.4.yml` (violation) and `sum.witness-2.4.yml` (correctness), in Python
- `concurrent-unreach.witness-2.2.yml`: a C violation with function enter, assumption, branching and target waypoints
- `safe-simple-concurrent.witness-2.1.yml`: a C correctness witness with ghost code and a location invariant
- `multivar_1-1.witness-2.1.yml`: a C correctness witness with a loop invariant and function contracts

They keep the layout and the license headers of that repository (Apache-2.0).
The files are kept byte for byte: the tests check the hashes written in the witnesses.
