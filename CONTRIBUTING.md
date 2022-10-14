# LiSA contribution guidelines #

Contributions to LiSA are always welcome! Thank you for taking time to make LiSA grow!

## Coding conventions ##

+ Always add an `@author` javadoc entry on the classes that you are creating, this helps contacting the appropriate person for clarifications and comments!
+ If you are implementing a solution defined in a scientific paper, please cite it at the top each file that is part of that implementation.
+ Keep the code readable!
	+ Use english for naming classes, methods, fields and variables.
	+ Avoid writing too long methods, too many nested loops or conditions.
	+ Smart comments are appreciated: every time you perform optimizations, or you take explicit implementation strategies that might be criptic, be sure to explain what you are doing.
	+ Write complete javadoc for everything that is not private: understanding the purpose of all code members is important for avoiding bugs
	+ Avoid runtime exceptions where possible, and try to stick with specific exception instances (e.g., avoid `RuntimeException("Illegal state")` in favor of `IllegalStateException()`)
	+ Spacing is important for readability!

## Making changes ##

+ Make sure people know the motivation of your work and can track its history even after you completed it: create an [issue][issues] describing what you are doing.
	+ Use the appropriate issue template, and be sure to fill every section as accurately as possible.
+ After forking this repository, create a _topic branch_ for you to work on.
	+ You should base your branch on the `master` branch.
	+ Name branch after issues: naming a branch `yourusername/#issuenbr-issue-topic` enables anyone to understand who is working on what in that branch.
	+ Try to keep different issues separated in different branch.
+ Commit messages should be clear for everyone, not just yourself.
	+ Commit related to an issue should start with the issue identifier: `#issuenbr fixing ...`.
	+ The commit message should be short and intuitive. If you feel the need for a longer explaination, separate the title of the commit from the full description with an empty line, and the fully elaborate your commit message.
+ Make sure you have added the necessary tests for your changes under in `src/test/java`.
+ Run a complete build with `gradle completeBuild` before creating a pull request! This will ensure that LiSA still builds fine and that all of the tests are passing.

## Submitting Changes ##

+ Push your changes to your topic branch.
+ Submit a _Pull Request_!
  + Check that the _Files Changed_ tab matches what you want to deliver.
  + Keep in mind that more file changed means more review time required.
+ Link the PR in the original issue.
+ Add the PR with the `work in progress` label if you do not want reviewers to work on it yet.

## Useful links ##

+ [GitHub docs](https://docs.github.com/)
+ [GitHub fork docs](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/about-forks)
+ [GitHub pull request docs](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request)
+ [LiSA issues][issues]

[issues]:https://github.com/lisa-analyzer/lisa/issues 
