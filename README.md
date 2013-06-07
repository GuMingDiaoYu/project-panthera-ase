# An Analytical SQL Engine (ASE) for Hadoop #

#### Project contact: [Shengsheng Huang] (mailto:shengsheng.huang@intel.com), [Jason Dai](mailto:jason.dai@intel.com)

---
### OVERVIEW ###

["Project Panthera"](<https://github.com/intel-hadoop/project-panthera>) is our open source efforts to enable efficient support of standard SQL features on Hadoop. While Higher level languages or APIs (such as Pig, Hive and Cascading) significantly lower the barrier to MapReduce, SQL is still the most important query language in modern business application environment, with more than 30 years of investments by the industry. There is a wealth of business users, enterprise analytics applications and third-party tools (such as query builders and BI applications) that all require full SQL support.

While there are continuous efforts in extending Hive’s SQL support, there are still large gaps in today’s Hadoop stack with regard to full SQL support. Many widely used SQL constructs in OLAP, such as nested subqueries, multiples-table select, etc., are still not supported by Hive/Hadoop.

Under "Project Panthera", we are building ASE, a new analytical SQL engine for Hadoop, whose goal is to provide full SQL support for OLAP applications on Hadoop. It enables complex and sophisticated SQL queries on Hadoop through a series of advanced transformations and optimizations (e.g., subquery unnesting), and currently uses Hive as its execution backend (with necessary enhancements to the underlying stack being added and contributed back to the existing Apache projects).

---
### Getting Started ###

- git clone https://github.com/intel-hadoop/project-panthera-ase.git
- cd project-panthera-ase/
- ant package
- cd build/dist/bin/
- ./hive
- hive>set hive.ql.mode=sql; (execute the command before every SQL statement)
- hive>select a from x where b > (select max(c) from y);
