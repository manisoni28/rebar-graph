package rebar.graph.aws;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import rebar.util.Json;
import rebar.util.Sleep;
import rebar.util.Json.JsonLogger;

public class SqsConsumerTest extends AwsIntegrationTest {

	
}
