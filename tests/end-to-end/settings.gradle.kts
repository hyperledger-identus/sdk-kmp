rootProject.name = "end-to-end"

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("org.hyperledger.identus:sdk")).using(project(":sdk"))
    }
}
