package fi.elsapalvelu.elsa.externalintegration.sisu.hy

enum class SisuHyEndpoint(
    val displayName: String,
    val query: String
) {
    STUDENT(
        "OpintotietodataSisuHy",
        """
            query OpintotietodataSisuHy(${"$"}id: ID!) {
                private_person_by_personal_identity_code(id: ${"$"}id) {
                    studentNumber
                    dateOfBirth
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
                        acceptedSelectionPath {
                            educationPhase1GroupId
                        }
                    }
                }
            }
        """.trimIndent()
    ),
    STUDY_ACCOMPLISHMENTS(
        "OpintosuorituksetSisuHy",
        """
            query OpintosuorituksetSisuHy(${"$"}id: ID!) {
                private_person_by_personal_identity_code(id: ${"$"}id) {
                    attainments {
                        state
                        attainmentDate
                        credits
                        courseUnitId
                        code
                        name { fi, sv }
                        module {
                            code
                            name { fi, sv }
                        }
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
                            courseUnitId
                            code
                            name { fi, sv }
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
