package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Administrator(int userId, String name, List<String> emails, int mediaId, long mediaVersionStamp, String lastLogin) {}