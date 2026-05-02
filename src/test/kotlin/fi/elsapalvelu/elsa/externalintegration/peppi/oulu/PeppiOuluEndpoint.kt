package fi.elsapalvelu.elsa.externalintegration.peppi.oulu

enum class PeppiOuluEndpoint(
    val displayName: String,
    val query: String
) {
    STUDENT(
        "OpintotietodataPeppiOulu",
        """
            query OpintotietodataPeppiOulu(${"$"}id: ID!) {
                private_person_by_personal_identity_code(id: ${"$"}id) {
                    dateOfBirth
                    studentNumber
                    studyRights {
                        id
                        valid {
                            startDate
                            endDate
                        }
                        state
                        phase1EducationClassificationUrn
                        decreeOnUniversityDegrees {
                            shortName { fi }
                        }
                        virtaPatevyyskoodi
                    }
                }
            }
        """.trimIndent()
    ),
    STUDY_ACCOMPLISHMENTS(
        "OpintosuorituksetPeppiOulu",
        """
            query OpintosuorituksetPeppiOulu(${"$"}id: ID!) {
                private_person_by_personal_identity_code(id: ${"$"}id) {
                    attainments {
                        state
                        attainmentDate
                        credits
                        courseUnit {
                            code
                            name { fi, sv }
                        }
                        grade {
                            name { fi, sv }
                            passed
                        }
                        expiryDate
                        studyRightId
                        childAttainments {
                            state
                            attainmentDate
                            credits
                            courseUnit {
                                code
                                name { fi, sv }
                            }
                            grade {
                                name { fi, sv }
                                passed
                            }
                            expiryDate
                        }
                    }
                }
            }
        """.trimIndent()
    )
}

