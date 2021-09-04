package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.Statement;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.superopt.TestHelper.optimizeStmt;

public class TestOptimizer {
  private static void doTest(String appName, int stmtId, String... expected) {
    final Statement stmt = Statement.findOne(appName, stmtId);
    System.out.println(stmt.parsed().toString(false));
    final Set<ASTNode> optimized = optimizeStmt(stmt);

    optimized.forEach(System.out::println);
    boolean passed = false;
    for (String s : expected)
      if (optimized.stream().anyMatch(it -> s.equals(it.toString()))) {
        passed = true;
        break;
      }
    assertTrue(passed);
  }

  @Test // 1
  void testBroadleaf199() {
    doTest(
        "broadleaf",
        199,
        "SELECT `adminrolei0_`.`admin_role_id` AS `admin_ro1_7_`, `adminrolei0_`.`created_by` AS `created_2_7_`, `adminrolei0_`.`date_created` AS `date_cre3_7_`, `adminrolei0_`.`date_updated` AS `date_upd4_7_`, `adminrolei0_`.`updated_by` AS `updated_5_7_`, `adminrolei0_`.`description` AS `descript6_7_`, `adminrolei0_`.`name` AS `name7_7_` FROM `blc_admin_role` AS `adminrolei0_` INNER JOIN `blc_admin_user_role_xref` AS `allusers1_` ON `adminrolei0_`.`admin_role_id` = `allusers1_`.`admin_role_id` WHERE `allusers1_`.`admin_user_id` = 1 ORDER BY `adminrolei0_`.`admin_role_id` ASC LIMIT 50");
  }

  @Test // 2
  void testBroadleaf200() {
    doTest(
        "broadleaf",
        200,
        "SELECT COUNT(`allusers1_`.`admin_role_id`) AS `col_0_0_` FROM `blc_admin_user_role_xref` AS `allusers1_` WHERE `allusers1_`.`admin_user_id` = 1");
  }

  @Test // 3
  void testBroadleaf201() {
    final String[] expectations = {
      "SELECT `adminpermi0_`.`admin_permission_id` AS `admin_pe1_4_`, `adminpermi0_`.`description` AS `descript2_4_`, `adminpermi0_`.`is_friendly` AS `is_frien3_4_`, `adminpermi0_`.`name` AS `name4_4_`, `adminpermi0_`.`permission_type` AS `permissi5_4_` FROM `blc_admin_permission` AS `adminpermi0_` INNER JOIN `blc_admin_user_permission_xref` AS `allusers1_` ON `adminpermi0_`.`admin_permission_id` = `allusers1_`.`admin_permission_id` WHERE `allusers1_`.`admin_user_id` = 1 AND `adminpermi0_`.`is_friendly` = 1 ORDER BY `adminpermi0_`.`description` ASC LIMIT 50"
    };
    doTest("broadleaf", 201, expectations);
  }

  @Test // 4
  void testBroadleaf241() {
    doTest(
        "broadleaf",
        241,
        "SELECT COUNT(`adminuseri0_`.`admin_user_id`) AS `col_0_0_` FROM `blc_admin_user` AS `adminuseri0_` WHERE `adminuseri0_`.`archived` = 'N' OR `adminuseri0_`.`archived` IS NULL");
  }

  @Test // 5
  void testDiaspora202() {
    final String[] expected = {
      "SELECT COUNT(`contacts`.`id`) FROM `contacts` AS `contacts` INNER JOIN `aspect_memberships` AS `aspect_memberships` ON `contacts`.`id` = `aspect_memberships`.`contact_id` WHERE `aspect_memberships`.`aspect_id` = 250 AND `contacts`.`user_id` = 332",
      "SELECT COUNT(`contacts`.`id`) FROM `contacts` AS `contacts` INNER JOIN `aspect_memberships` AS `aspect_memberships` ON `contacts`.`id` = `aspect_memberships`.`contact_id` WHERE `contacts`.`user_id` = 332 AND `aspect_memberships`.`aspect_id` = 250"
    };

    doTest("diaspora", 202, expected);
  }

  // 6 diaspora-224 slow

  @Test // 7
  void testDiaspora295() {
    final String appName = "diaspora";
    final int stmtId = 295;
    final String expected =
        "SELECT COUNT(`contacts`.`id`) FROM `contacts` AS `contacts` WHERE `contacts`.`user_id` = 1945";
    doTest(appName, stmtId, expected);
  }

  @Test // 8
  void testDiaspora460() {
    final String appName = "diaspora";
    final int stmtId = 460;
    final String[] expected = {
      "SELECT DISTINCT `contacts`.`person_id` AS `person_id` FROM `contacts` AS `contacts` INNER JOIN `aspect_memberships` AS `aspect_memberships` ON `contacts`.`id` = `aspect_memberships`.`contact_id`"
    };
    doTest(appName, stmtId, expected);
  }

  @Test // 9
  void testDiaspora478() {
    final String appName = "diaspora";
    final int stmtId = 478;
    // TODO!
    final String[] expected = {
      "SELECT DISTINCT `profiles`.`last_name` AS `alias_0`, `contacts`.`id` AS `id` FROM `contacts` AS `contacts` LEFT JOIN `people` AS `people` ON `contacts`.`person_id` = `people`.`id` LEFT JOIN `profiles` AS `profiles` ON `people`.`id` = `profiles`.`person_id` WHERE `contacts`.`user_id` = 3 AND `contacts`.`receiving` = TRUE ORDER BY `profiles`.`last_name` ASC LIMIT 25 OFFSET 0"
    };

    doTest(appName, stmtId, expected);
  }

  @Test // 10
  void testDiaspora492() {
    final String appName = "diaspora";
    final int stmtId = 492;
    final String[] expected = {
      "SELECT COUNT(*) FROM `conversation_visibilities` AS `conversation_visibilities` WHERE `conversation_visibilities`.`person_id` = 2",
    };

    doTest(appName, stmtId, expected);
  }

  @Test // 11
  void testDiscourse123() {
    final String appName = "discourse";
    final int stmtId = 123;
    final String[] expected = {
      "SELECT DISTINCT \"category_groups\".\"category_id\" AS \"category_id\" FROM \"category_groups\" AS \"category_groups\" INNER JOIN \"group_users\" AS \"group_users\" ON \"category_groups\".\"group_id\" = \"group_users\".\"group_id\" WHERE \"group_users\".\"user_id\" = 86"
    };
    doTest(appName, stmtId, expected);
  }

  @Test // 12
  void testDiscourse182() {
    final String appName = "discourse";
    final int stmtId = 182;
    final String expected =
        "SELECT \"topic_allowed_groups\".\"group_id\" AS \"group_id\" FROM \"topic_allowed_groups\" AS \"topic_allowed_groups\" WHERE \"topic_allowed_groups\".\"topic_id\" = 15596";
    doTest(appName, stmtId, expected);
  }

  @Test // 13
  void testDiscourse184() {
    final String appName = "discourse";
    final int stmtId = 184;
    final String expected =
        "SELECT \"topic_tags\".\"tag_id\" AS \"tag_id\" FROM \"topic_tags\" AS \"topic_tags\" WHERE \"topic_tags\".\"topic_id\" = 15596";
    doTest(appName, stmtId, expected);
  }

  @Test // 14
  void testDiscourse207() {
    final String appName = "discourse";
    final int stmtId = 207;
    final String[] expected = {
      "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE (\"posts\".\"user_id\" = -1 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15601 AND \"posts\".\"id\" = 16384 ORDER BY \"posts\".\"sort_order\"",
    };
    doTest(appName, stmtId, expected);
  }

  @Test // 15
  void testDiscourse276() {
    final String appName = "discourse";
    final int stmtId = 276;
    final String expected =
        "SELECT \"ignored_users\".\"ignored_user_id\" AS \"ignored_user_id\" FROM \"ignored_users\" AS \"ignored_users\" WHERE \"ignored_users\".\"user_id\" = 155";
    doTest(appName, stmtId, expected);
  }

  @Test // 16
  void testDiscourse277() {
    final String appName = "discourse";
    final int stmtId = 277;
    final String expected =
        "SELECT \"muted_users\".\"muted_user_id\" AS \"muted_user_id\" FROM \"muted_users\" AS \"muted_users\" WHERE \"muted_users\".\"user_id\" = 155";
    doTest(appName, stmtId, expected);
  }

  @Test // 17
  void testDiscourse371() {
    final String appName = "discourse";
    final int stmtId = 371;
    final String expected =
        "SELECT \"topic_allowed_users\".\"user_id\" AS \"user_id\" FROM \"topic_allowed_users\" AS \"topic_allowed_users\" WHERE \"topic_allowed_users\".\"topic_id\" = 15632";
    doTest(appName, stmtId, expected);
  }

  @Test // 18
  void testDiscourse373() {
    final String appName = "discourse";
    final int stmtId = 373;
    final String expected =
        "SELECT \"group_users\".\"user_id\" AS \"user_id\" FROM \"group_users\" AS \"group_users\" WHERE \"group_users\".\"group_id\" = 2";
    doTest(appName, stmtId, expected);
  }

  @Test // 19
  void testDiscourse417() {
    final String appName = "discourse";
    final int stmtId = 417;
    final String[] expected =
        new String[] {
          "SELECT \"category_users\".\"user_id\" AS \"user_id\" FROM \"category_users\" AS \"category_users\" WHERE \"category_users\".\"notification_level\" = 4 AND \"category_users\".\"category_id\" IS NULL",
          "SELECT \"category_users\".\"user_id\" AS \"user_id\" FROM \"category_users\" AS \"category_users\" WHERE \"category_users\".\"category_id\" IS NULL AND \"category_users\".\"notification_level\" = 4"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 20
  void testDiscourse449() {
    final String appName = "discourse";
    final int stmtId = 449;
    final String[] expected =
        new String[] {
          "SELECT \"child_themes\".\"parent_theme_id\" AS \"parent_theme_id\" FROM \"child_themes\" AS \"child_themes\" WHERE \"child_themes\".\"child_theme_id\" = 1017",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 21
  void testDiscourse599() {
    final String appName = "discourse";
    final int stmtId = 599;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"category_tags\" AS \"category_tags\" WHERE \"category_tags\".\"category_id\" = 3121",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 22
  void testDiscourse600() {
    final String appName = "discourse";
    final int stmtId = 600;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"category_tag_groups\" AS \"category_tag_groups\" WHERE \"category_tag_groups\".\"category_id\" = 3121",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 23
  void testDiscourse624() {
    final String appName = "discourse";
    final int stmtId = 624;
    final String[] expected =
        new String[] {
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" WHERE \"group_users\".\"user_id\" = 247",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 24
  void testDiscourse660() {
    final String appName = "discourse";
    final int stmtId = 660;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"category_groups\" AS \"category_groups\" WHERE \"category_groups\".\"group_id\" = 2378",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 25
  void testDiscourse833() {
    final String appName = "discourse";
    final int stmtId = 833;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"group_users\" AS \"group_users\" WHERE \"group_users\".\"group_id\" = 2397",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 26
  void testDiscourse877() {
    final String appName = "discourse";
    final int stmtId = 877;
    final String[] expected =
        new String[] {
          "SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" INNER JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" INNER JOIN \"group_users\" AS \"group_users\" ON \"posts\".\"user_id\" = \"group_users\".\"user_id\" WHERE \"group_users\".\"group_id\" = 2412 AND \"topics\".\"visible\" = TRUE AND NOT \"topics\".\"deleted_at\" IS NULL AND (\"topics\".\"category_id\" IS NULL OR \"topics\".\"category_id\" IN ($1)) AND \"topics\".\"archetype\" <> 'private_message' AND \"posts\".\"post_type\" = 1 AND NOT \"posts\".\"deleted_at\" IS NULL ORDER BY \"posts\".\"created_at\" DESC LIMIT 50"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 27
  void testDiscourse942() {
    final String appName = "discourse";
    final int stmtId = 942;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"group_users\" AS \"gu\" WHERE \"gu\".\"owner\" = TRUE AND \"gu\".\"user_id\" = 779 AND \"gu\".\"group_id\" > 0",
          "SELECT COUNT(*) FROM \"group_users\" AS \"gu\" WHERE \"gu\".\"group_id\" > 0 AND \"gu\".\"user_id\" = 779 AND \"gu\".\"owner\" = TRUE"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 28
  void testDiscourse944() {
    final String appName = "discourse";
    final int stmtId = 944;
    final String[] expected =
        new String[] {
          "SELECT \"gu\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" INNER JOIN \"group_users\" AS \"gu\" ON \"group_users\".\"group_id\" = \"gu\".\"group_id\" WHERE \"gu\".\"group_id\" > 0 AND \"gu\".\"user_id\" = 779 AND \"group_users\".\"user_id\" = 779",
          "SELECT \"gu\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"gu\" INNER JOIN \"group_users\" AS \"group_users\" ON \"gu\".\"group_id\" = \"group_users\".\"group_id\" WHERE \"group_users\".\"group_id\" > 0 AND \"gu\".\"user_id\" = 779",
          "SELECT \"gu\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"gu\" INNER JOIN \"group_users\" AS \"group_users\" ON \"gu\".\"group_id\" = \"group_users\".\"group_id\" WHERE \"gu\".\"group_id\" > 0 AND \"gu\".\"user_id\" = 779",
          "SELECT \"gu\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" INNER JOIN \"group_users\" AS \"gu\" ON \"group_users\".\"group_id\" = \"gu\".\"group_id\" WHERE \"gu\".\"group_id\" > 0 AND \"gu\".\"user_id\" = 779"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 29
  void testDiscourse945() {
    final String appName = "discourse";
    final int stmtId = 945;
    final String[] expected =
        new String[] {
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"gu\" INNER JOIN \"group_users\" AS \"group_users\" ON \"gu\".\"group_id\" = \"group_users\".\"group_id\" WHERE \"gu\".\"group_id\" > 0 AND \"group_users\".\"owner\" = TRUE AND \"gu\".\"user_id\" = 779",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" INNER JOIN \"group_users\" AS \"gu\" ON \"group_users\".\"group_id\" = \"gu\".\"group_id\" WHERE \"gu\".\"group_id\" > 0 AND \"group_users\".\"owner\" = TRUE AND \"gu\".\"user_id\" = 779",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" INNER JOIN \"group_users\" AS \"gu\" ON \"group_users\".\"group_id\" = \"gu\".\"group_id\" WHERE \"group_users\".\"group_id\" > 0 AND \"group_users\".\"owner\" = TRUE AND \"gu\".\"user_id\" = 779",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"gu\" INNER JOIN \"group_users\" AS \"group_users\" ON \"gu\".\"group_id\" = \"group_users\".\"group_id\" WHERE \"group_users\".\"group_id\" > 0 AND \"group_users\".\"owner\" = TRUE AND \"gu\".\"user_id\" = 779"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 30
  void testDiscourse946() {
    final String appName = "discourse";
    final int stmtId = 946;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"group_users\" AS \"gu\" WHERE \"gu\".\"user_id\" = 779 AND \"gu\".\"group_id\" > 0",
          "SELECT COUNT(*) FROM \"group_users\" AS \"gu\" WHERE \"gu\".\"group_id\" > 0 AND \"gu\".\"user_id\" = 779"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 31
  void testDiscourse948() {
    final String appName = "discourse";
    final int stmtId = 948;
    // TODO!
    final String[] expected =
        new String[] {
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"groups\" AS \"groups\" INNER JOIN \"group_users\" AS \"group_users\" ON \"groups\".\"id\" = \"group_users\".\"group_id\" WHERE \"groups\".\"id\" > 0 AND \"groups\".\"automatic\" = TRUE AND \"group_users\".\"user_id\" = 779",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"groups\" AS \"groups\" INNER JOIN \"group_users\" AS \"group_users\" ON \"groups\".\"id\" = \"group_users\".\"group_id\" WHERE \"group_users\".\"group_id\" > 0 AND \"groups\".\"automatic\" = TRUE AND \"group_users\".\"user_id\" = 779",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" INNER JOIN \"groups\" AS \"groups\" ON \"group_users\".\"group_id\" = \"groups\".\"id\" WHERE \"groups\".\"id\" > 0 AND \"groups\".\"automatic\" = TRUE AND \"group_users\".\"user_id\" = 779",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" INNER JOIN \"groups\" AS \"groups\" ON \"group_users\".\"group_id\" = \"groups\".\"id\" WHERE \"group_users\".\"group_id\" > 0 AND \"groups\".\"automatic\" = TRUE AND \"group_users\".\"user_id\" = 779",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 32
  void testDiscourse949() {
    final String appName = "discourse";
    final int stmtId = 949;
    final String[] expected =
        new String[] {
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" INNER JOIN \"groups\" AS \"groups\" ON \"group_users\".\"group_id\" = \"groups\".\"id\" WHERE \"group_users\".\"group_id\" > 0 AND \"groups\".\"automatic\" = TRUE AND \"group_users\".\"user_id\" = 779 AND \"group_users\".\"owner\" = TRUE",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"groups\" AS \"groups\" INNER JOIN \"group_users\" AS \"group_users\" ON \"groups\".\"id\" = \"group_users\".\"group_id\" WHERE \"groups\".\"id\" > 0 AND \"groups\".\"automatic\" = TRUE AND \"group_users\".\"user_id\" = 779 AND \"group_users\".\"owner\" = TRUE",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"groups\" AS \"groups\" INNER JOIN \"group_users\" AS \"group_users\" ON \"groups\".\"id\" = \"group_users\".\"group_id\" WHERE \"group_users\".\"group_id\" > 0 AND \"groups\".\"automatic\" = TRUE AND \"group_users\".\"user_id\" = 779 AND \"group_users\".\"owner\" = TRUE",
          "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM \"group_users\" AS \"group_users\" INNER JOIN \"groups\" AS \"groups\" ON \"group_users\".\"group_id\" = \"groups\".\"id\" WHERE \"groups\".\"id\" > 0 AND \"groups\".\"automatic\" = TRUE AND \"group_users\".\"user_id\" = 779 AND \"group_users\".\"owner\" = TRUE"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 33
  void testDiscourse994() {
    final String appName = "discourse";
    final int stmtId = 994;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"group_users\" AS \"gu\" WHERE \"gu\".\"user_id\" = 780 AND \"gu\".\"group_id\" > 0",
          "SELECT COUNT(*) FROM \"group_users\" AS \"gu\" WHERE \"gu\".\"group_id\" > 0 AND \"gu\".\"user_id\" = 780"
        };
    doTest(appName, stmtId, expected);
  }

  // 34 discourse-1000 slow

  @Test // 35
  void testDiscourse1003() {
    final String appName = "discourse";
    final int stmtId = 1003;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"group_requests\" AS \"group_requests\" WHERE \"group_requests\".\"group_id\" = 2563",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 36
  void testDiscourse1006() {
    final String appName = "discourse";
    final int stmtId = 1006;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"group_users\" AS \"group_users\" WHERE \"group_users\".\"group_id\" = 2564 AND \"group_users\".\"user_id\" > 0",
          "SELECT COUNT(*) FROM \"group_users\" AS \"group_users\" WHERE \"group_users\".\"user_id\" > 0 AND \"group_users\".\"group_id\" = 2564"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 37
  void testDiscourse1048() {
    final String appName = "discourse";
    final int stmtId = 1048;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS \"one\" FROM \"group_histories\" AS \"group_histories\" WHERE \"group_histories\".\"group_id\" = 2576 LIMIT 25 OFFSET 0) AS \"subquery_for_count\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 38
  void testDiscourse1173() {
    final String appName = "discourse";
    final int stmtId = 1173;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE (\"posts\".\"user_id\" = 913 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15986 ORDER BY \"posts\".\"sort_order\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 39
  void testDiscourse1174() {
    final String appName = "discourse";
    final int stmtId = 1174;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE (\"posts\".\"user_id\" = 915 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15986 AND \"posts\".\"id\" IN ($1) AND NOT \"posts\".\"deleted_at\" IS NULL ORDER BY \"posts\".\"sort_order\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 40
  void testDiscourse1178() {
    final String appName = "discourse";
    final int stmtId = 1178;
    final String[] expected =
        new String[] {
          "SELECT \"posts_0\".\"id\" AS \"id\", \"posts_0\".\"post_number\" AS \"post_number\" FROM (SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" WHERE (\"posts\".\"user_id\" = 915 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15986 AND \"posts\".\"post_type\" = 1 AND NOT \"posts\".\"deleted_at\" IS NULL AND \"posts\".\"post_number\" > 1 ORDER BY \"posts\".\"percent_rank\" ASC, \"posts\".\"sort_order\" ASC LIMIT 2) AS \"posts_0\" WHERE NOT \"posts_0\".\"deleted_at\" IS NULL ORDER BY \"posts_0\".\"post_number\" ASC"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 41
  void testDiscourse1179() {
    final String appName = "discourse";
    final int stmtId = 1179;
    final String[] expected =
        new String[] {
          "SELECT \"posts_0\".\"id\" AS \"id\" FROM (SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" WHERE \"posts\".\"topic_id\" = 15986 AND \"posts\".\"post_type\" = 1 AND \"posts\".\"post_type\" IN ($1) AND NOT \"posts\".\"deleted_at\" IS NULL AND \"posts\".\"post_number\" > 1 ORDER BY \"posts\".\"percent_rank\" ASC, \"posts\".\"sort_order\" ASC LIMIT 99) AS \"posts_0\" WHERE NOT \"posts_0\".\"deleted_at\" IS NULL ORDER BY \"posts_0\".\"post_number\" ASC"
        };
    doTest(appName, stmtId, expected);
  }

  // 42 discourse 1181 wrong

  @Test // 43
  void testDiscourse1182() {
    final String appName = "discourse";
    final int stmtId = 1182;
    final String[] expected =
        new String[] {
          "SELECT \"posts_0\".\"id\" AS \"id\" FROM (SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" WHERE \"posts\".\"topic_id\" = 15986 AND \"posts\".\"score\" >= 99 AND \"posts\".\"post_type\" = 1 AND \"posts\".\"post_type\" IN ($1) AND NOT \"posts\".\"deleted_at\" IS NULL AND \"posts\".\"post_number\" > 1 ORDER BY \"posts\".\"percent_rank\" ASC, \"posts\".\"sort_order\" ASC LIMIT 99) AS \"posts_0\" WHERE NOT \"posts_0\".\"deleted_at\" IS NULL ORDER BY \"posts_0\".\"post_number\" ASC"
        };
    doTest(appName, stmtId, expected);
  }

  // 44 discourse-1183 wrong

  @Test // 45
  void testDiscourse1186() {
    final String appName = "discourse";
    final int stmtId = 1186;
    final String[] expected =
        new String[] {
          "SELECT \"posts_0\".\"id\" AS \"id\" FROM (SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" WHERE \"posts\".\"topic_id\" = 15986 AND \"posts\".\"post_type\" = 1 AND \"posts\".\"post_type\" IN ($1) AND \"posts\".\"id\" = 16810 AND NOT \"posts\".\"deleted_at\" IS NULL AND \"posts\".\"post_number\" > 1 ORDER BY \"posts\".\"percent_rank\" ASC, \"posts\".\"sort_order\" ASC LIMIT 99) AS \"posts_0\" WHERE NOT \"posts_0\".\"deleted_at\" IS NULL ORDER BY \"posts_0\".\"post_number\" ASC"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 46
  void testDiscourse1191() {
    final String appName = "discourse";
    final int stmtId = 1191;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE \"posts\".\"topic_id\" = 15986 AND \"posts\".\"post_type\" IN ($1) AND \"posts\".\"id\" IN ($1) AND NOT \"posts\".\"deleted_at\" IS NULL ORDER BY \"posts\".\"sort_order\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 47
  void testDiscourse1196() {
    final String appName = "discourse";
    final int stmtId = 1196;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE (\"posts\".\"user_id\" = 915 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15986 AND \"posts\".\"id\" IN ($1) AND NOT \"posts\".\"deleted_at\" IS NULL ORDER BY \"posts\".\"sort_order\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 48
  void testDiscourse1200() {
    final String appName = "discourse";
    final int stmtId = 1200;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE (\"posts\".\"user_id\" = 912 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15986 AND \"posts\".\"id\" IN ($1) ORDER BY \"posts\".\"sort_order\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 49
  void testDiscourse1213() {
    final String appName = "discourse";
    final int stmtId = 1213;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE (\"posts\".\"user_id\" = 915 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15986 AND \"posts\".\"id\" = 16887 AND NOT \"posts\".\"deleted_at\" IS NULL ORDER BY \"posts\".\"sort_order\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 50
  void testDiscourse1214() {
    final String appName = "discourse";
    final int stmtId = 1214;
    final String[] expected =
        new String[] {
          "SELECT \"posts_0\".\"id\" AS \"id\" FROM (SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" WHERE (\"posts\".\"user_id\" = 915 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15986 AND \"posts\".\"post_type\" = 1 AND NOT \"posts\".\"deleted_at\" IS NULL AND \"posts\".\"post_number\" > 1 ORDER BY \"posts\".\"percent_rank\" ASC, \"posts\".\"sort_order\" ASC LIMIT 5) AS \"posts_0\" WHERE NOT \"posts_0\".\"deleted_at\" IS NULL ORDER BY \"posts_0\".\"post_number\" ASC",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 51
  void testDiscourse1216() {
    final String appName = "discourse";
    final int stmtId = 1216;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE (\"posts\".\"user_id\" = 915 OR \"posts\".\"post_type\" IN ($1)) AND \"posts\".\"topic_id\" = 15986 AND \"posts\".\"id\" IN ($1) AND NOT \"posts\".\"deleted_at\" IS NULL ORDER BY \"posts\".\"sort_order\" DESC"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 52
  void testDiscourse1291() {
    final String appName = "discourse";
    final int stmtId = 1291;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"child_themes\".\"parent_theme_id\" AS \"parent_theme_id\" FROM \"child_themes\" AS \"child_themes\" WHERE \"child_themes\".\"child_theme_id\" IN ($1)",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 53
  void testDiscourse1361() {
    final String appName = "discourse";
    final int stmtId = 1361;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"tags\".\"name\" AS \"name\" FROM \"tags\" AS \"tags\" INNER JOIN \"tag_group_memberships\" AS \"tag_group_memberships\" ON \"tags\".\"id\" = \"tag_group_memberships\".\"tag_id\" INNER JOIN \"tag_group_permissions\" AS \"tag_group_permissions\" ON \"tag_group_memberships\".\"tag_group_id\" = \"tag_group_permissions\".\"tag_group_id\" WHERE \"tag_group_permissions\".\"group_id\" = 0 AND \"tag_group_permissions\".\"permission_type\" = 3",
          "SELECT DISTINCT \"tags\".\"name\" AS \"name\" FROM \"tags\" AS \"tags\" INNER JOIN \"tag_group_memberships\" AS \"tag_group_memberships\" ON \"tags\".\"id\" = \"tag_group_memberships\".\"tag_id\" INNER JOIN \"tag_group_permissions\" AS \"tag_group_permissions\" ON \"tag_group_memberships\".\"tag_group_id\" = \"tag_group_permissions\".\"tag_group_id\" WHERE \"tag_group_permissions\".\"permission_type\" = 3 AND \"tag_group_permissions\".\"group_id\" = 0"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 54
  void testDiscourse1426() {
    final String appName = "discourse";
    final int stmtId = 1426;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"topic_allowed_users\" AS \"topic_allowed_users\" WHERE \"topic_allowed_users\".\"topic_id\" = 16056",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 55
  void testDiscourse1473() {
    final String appName = "discourse";
    final int stmtId = 1473;
    final String[] expected =
        new String[] {
          "SELECT \"tag_group_memberships\".\"tag_id\" AS \"tag_id\" FROM \"tag_group_memberships\" AS \"tag_group_memberships\" WHERE \"tag_group_memberships\".\"tag_group_id\" = 453",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 56
  void testDiscourse1957() {
    final String appName = "discourse";
    final int stmtId = 1957;
    final String[] expected =
        new String[] {
          "SELECT \"user_emails\".\"email\" AS \"email\" FROM \"topic_allowed_users\" AS \"topic_allowed_users\" INNER JOIN \"user_emails\" AS \"user_emails\" ON \"topic_allowed_users\".\"user_id\" = \"user_emails\".\"user_id\" WHERE \"topic_allowed_users\".\"topic_id\" = 16471",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 57
  void testDiscourse2012() {
    final String appName = "discourse";
    final int stmtId = 2012;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE \"posts\".\"topic_id\" = 16515 AND \"posts\".\"post_type\" IN ($1) AND \"posts\".\"id\" = 17616 AND NOT \"posts\".\"deleted_at\" IS NULL ORDER BY \"posts\".\"sort_order\""
        };
    doTest(appName, stmtId, expected);
  }

  // 58 discourse-2016 wrong

  @Test // 59
  void testDiscourse2019() {
    final String appName = "discourse";
    final int stmtId = 2019;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"id\" AS \"id\", \"posts\".\"sort_order\" AS \"sort_order\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE \"posts\".\"topic_id\" = 16521 AND \"posts\".\"post_type\" IN ($1) AND NOT \"posts\".\"deleted_at\" IS NULL ORDER BY \"posts\".\"sort_order\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 60
  void testDiscourse2291() {
    final String appName = "discourse";
    final int stmtId = 2291;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM \"post_uploads\" AS \"post_uploads\"",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 61
  void testDiscourse2407() {
    final String appName = "discourse";
    final int stmtId = 2407;
    final String[] expected =
        // TODO
        new String[] {
          "SELECT DISTINCT \"child_themes\".\"parent_theme_id\" AS \"parent_theme_id\" FROM \"child_themes\" AS \"child_themes\"",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 62
  void testDiscourse2783() {
    final String appName = "discourse";
    final int stmtId = 2783;
    final String[] expected =
        new String[] {
          "SELECT \"topic_allowed_groups\".\"group_id\" AS \"group_id\" FROM \"topic_allowed_groups\" AS \"topic_allowed_groups\" WHERE \"topic_allowed_groups\".\"group_id\" IN ($1) AND \"topic_allowed_groups\".\"topic_id\" = 17701",
        };
    doTest(appName, stmtId, expected);
  }

  //  @Test // 63 // Slow
  void testDiscourse2832() {
    final String appName = "discourse";
    final int stmtId = 2832;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"categories\".\"name\" AS \"name\" FROM \"categories\" AS \"categories\" INNER JOIN \"category_tag_groups\" AS \"category_tag_groups\" ON \"categories\".\"id\" = \"category_tag_groups\".\"category_id\" INNER JOIN \"tag_group_memberships\" AS \"tag_group_memberships\" ON \"category_tag_groups\".\"tag_group_id\" = \"tag_group_memberships\".\"tag_group_id\" WHERE \"categories\".\"id\" IN ($1) AND \"tag_group_memberships\".\"tag_id\" = 1771"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 64
  void testDiscourse2839() {
    final String appName = "discourse";
    final int stmtId = 2839;
    final String[] expected =
        new String[] {
          "SELECT \"category_tags\".\"category_id\" AS \"category_id\" FROM \"category_tags\" AS \"category_tags\" WHERE \"category_tags\".\"tag_id\" = 1775",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 65
  void testDiscourse3638() {
    final String appName = "discourse";
    final int stmtId = 3638;
    final String[] expected =
        new String[] {
          "SELECT \"groups_web_hooks\".\"group_id\" AS \"group_id\" FROM \"groups_web_hooks\" AS \"groups_web_hooks\" WHERE \"groups_web_hooks\".\"web_hook_id\" = 182",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 66
  void testDiscourse3639() {
    final String appName = "discourse";
    final int stmtId = 3639;
    final String[] expected =
        new String[] {
          "SELECT \"categories_web_hooks\".\"category_id\" AS \"category_id\" FROM \"categories_web_hooks\" AS \"categories_web_hooks\" WHERE \"categories_web_hooks\".\"web_hook_id\" = 182",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 67
  void testDiscourse3640() {
    final String appName = "discourse";
    final int stmtId = 3640;
    final String[] expected =
        new String[] {
          "SELECT \"tags_web_hooks\".\"tag_id\" AS \"tag_id\" FROM \"tags_web_hooks\" AS \"tags_web_hooks\" WHERE \"tags_web_hooks\".\"web_hook_id\" = 182",
        };
    doTest(appName, stmtId, expected);
  }

  //  @Test // 68
  void testDiscourse3690() {
    final String appName = "discourse";
    final int stmtId = 3690;
    final String[] expected =
        new String[] {
          "SELECT COUNT(\"directory_items\".\"id\") FROM \"directory_items\" AS \"directory_items\" LEFT JOIN \"users\" AS \"users\" ON \"directory_items\".\"user_id\" = \"users\".\"id\" INNER JOIN \"group_users\" AS \"group_users\" ON \"users\".\"id\" = \"group_users\".\"user_id\" WHERE \"directory_items\".\"period_type\" = 1 AND \"group_users\".\"group_id\" = 2898"
        };
    doTest(appName, stmtId, expected);
  }

  //  @Test // 69 // Slow
  void testDiscourse3691() {
    final String appName = "discourse";
    final int stmtId = 3691;
    final String[] expected =
        new String[] {
          "SELECT \"directory_items\".\"likes_received\" AS \"alias_0\", \"directory_items\".\"id\" AS \"id\" FROM \"directory_items\" AS \"directory_items\" LEFT JOIN \"users\" AS \"users\" ON \"directory_items\".\"user_id\" = \"users\".\"id\" INNER JOIN \"group_users\" AS \"group_users\" ON \"users\".\"id\" = \"group_users\".\"user_id\" WHERE \"directory_items\".\"period_type\" = 1 AND \"group_users\".\"group_id\" = 2898 ORDER BY \"directory_items\".\"likes_received\" DESC LIMIT 50 OFFSET 0"
        };
    doTest(appName, stmtId, expected);
  }

  // 70 discourse-3825 slow
  // 71 discourse-3829 slow
  // 72 discourse-3831 slow
  // 73 discourse-3842 slow

  @Test // 74
  void testDiscourse4071() {
    final String appName = "discourse";
    final int stmtId = 4071;
    final String[] expected =
        new String[] {
          "SELECT \"topic_allowed_users\".\"user_id\" AS \"user_id\" FROM \"topic_allowed_users\" AS \"topic_allowed_users\" WHERE \"topic_allowed_users\".\"topic_id\" = 18844",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 75
  void testDiscourse4156() {
    final String appName = "discourse";
    final int stmtId = 4156;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"created_at\" AS \"created_at\" FROM \"posts\" AS \"posts\" LEFT JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" AND NOT \"topics\".\"deleted_at\" IS NULL WHERE \"posts\".\"user_id\" = 7304 ORDER BY \"posts\".\"created_at\" DESC"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 76
  void testDiscourse5044() {
    final String appName = "discourse";
    final int stmtId = 5044;
    final String[] expected =
        new String[] {
          "SELECT \"category_search_data\".\"category_id\" AS \"category_id\" FROM \"category_search_data\" AS \"category_search_data\" WHERE \"category_search_data\".\"locale\" <> 'fr' OR \"category_search_data\".\"version\" <> 3 ORDER BY \"category_search_data\".\"category_id\" ASC LIMIT 500"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 77
  void testEladmin104() {
    final String appName = "eladmin";
    final int stmtId = 104;
    final String[] expected =
        new String[] {
          "SELECT `job0_`.`id` AS `id1_6_`, `job0_`.`create_time` AS `create_t2_6_`, `job0_`.`dept_id` AS `dept_id6_6_`, `job0_`.`enabled` AS `enabled3_6_`, `job0_`.`name` AS `name4_6_`, `job0_`.`sort` AS `sort5_6_` FROM `job` AS `job0_` ORDER BY `job0_`.`sort` ASC LIMIT 10"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 78
  void testEladmin105() {
    final String appName = "eladmin";
    final int stmtId = 105;
    final String[] expected =
        new String[] {
          "SELECT `job0_`.`id` AS `id1_6_`, `job0_`.`create_time` AS `create_t2_6_`, `job0_`.`dept_id` AS `dept_id6_6_`, `job0_`.`enabled` AS `enabled3_6_`, `job0_`.`name` AS `name4_6_`, `job0_`.`sort` AS `sort5_6_` FROM `job` AS `job0_` WHERE `job0_`.`name` LIKE '%d%' ORDER BY `job0_`.`sort` ASC LIMIT 10"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 79
  void testFatfreecrm9() {
    final String appName = "fatfreecrm";
    final int stmtId = 9;
    final String[] expected =
        new String[] {
          "SELECT `taggings`.`tag_id` AS `tag_id` FROM `taggings` AS `taggings` WHERE `taggings`.`context` = 'tags' AND `taggings`.`taggable_id` = 1234 AND `taggings`.`taggable_type` = 'Contact'",
          "SELECT `taggings`.`tag_id` AS `tag_id` FROM `taggings` AS `taggings` WHERE `taggings`.`taggable_type` = 'Contact' AND `taggings`.`taggable_id` = 1234 AND `taggings`.`context` = 'tags'"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 80
  void testFatfreecrm16() {
    final String appName = "fatfreecrm";
    final int stmtId = 16;
    final String[] expected =
        new String[] {
          "SELECT `groups_users`.`group_id` AS `group_id` FROM `groups_users` AS `groups_users` WHERE `groups_users`.`user_id` = 3056",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 81
  void testFatfreecrm19() {
    final String appName = "fatfreecrm";
    final int stmtId = 19;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `accounts` AS `accounts` WHERE `accounts`.`assigned_to` = 237 OR (`accounts`.`user_id` = 237 OR `accounts`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 82
  void testFatfreecrm26() {
    final String appName = "fatfreecrm";
    final int stmtId = 26;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `contacts` AS `contacts` WHERE `contacts`.`assigned_to` = 688 OR (`contacts`.`user_id` = 688 OR `contacts`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 83
  void testFatfreecrm32() {
    final String appName = "fatfreecrm";
    final int stmtId = 32;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `accounts` AS `accounts` WHERE `accounts`.`category` IN (?) AND (`accounts`.`assigned_to` = 238 OR (`accounts`.`user_id` = 238 OR `accounts`.`access` = 'Public'))"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 84
  void testFatfreecrm33() {
    final String appName = "fatfreecrm";
    final int stmtId = 33;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `leads` AS `leads` WHERE `leads`.`status` IN (?) AND (`leads`.`assigned_to` = 1131 OR (`leads`.`user_id` = 1131 OR `leads`.`access` = 'Public'))"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 85
  void testFatfreecrm80() {
    final String appName = "fatfreecrm";
    final int stmtId = 80;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE `opportunities`.`assigned_to` = 1496 OR (`opportunities`.`user_id` = 1496 OR `opportunities`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 86
  void testFatfreecrm87() {
    final String appName = "fatfreecrm";
    final int stmtId = 87;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `leads` AS `leads` WHERE `leads`.`assigned_to` = 980 OR (`leads`.`user_id` = 980 OR `leads`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 87
  void testFatfreecrm94() {
    final String appName = "fatfreecrm";
    final int stmtId = 94;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `accounts` AS `accounts` WHERE (`accounts`.`name` LIKE '%second%' OR `accounts`.`email` LIKE '%second%') AND (`accounts`.`assigned_to` = 239 OR (`accounts`.`user_id` = 239 OR `accounts`.`access` = 'Public'))"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 88
  void testFatfreecrm96() {
    final String appName = "fatfreecrm";
    final int stmtId = 96;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `accounts` AS `accounts` WHERE `accounts`.`assigned_to` = 358 OR (`accounts`.`user_id` = 358 OR `accounts`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 89
  void testFatfreecrm112() {
    final String appName = "fatfreecrm";
    final int stmtId = 112;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `campaigns` AS `campaigns` WHERE `campaigns`.`assigned_to` = 410 OR (`campaigns`.`user_id` = 410 OR `campaigns`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 90
  void testFatfreecrm147() {
    final String appName = "fatfreecrm";
    final int stmtId = 147;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE `opportunities`.`assigned_to` = 1359 OR (`opportunities`.`user_id` = 1359 OR `opportunities`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 91
  void testFatfreecrm149() {
    final String appName = "fatfreecrm";
    final int stmtId = 149;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE `opportunities`.`assigned_to` = 1359 OR (`opportunities`.`user_id` = 1359 OR `opportunities`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 92
  void testFatfreecrm152() {
    final String appName = "fatfreecrm";
    final int stmtId = 152;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `campaigns` AS `campaigns` WHERE `campaigns`.`status` IN (?) AND (`campaigns`.`assigned_to` = 363 OR (`campaigns`.`user_id` = 363 OR `campaigns`.`access` = 'Public'))"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 93
  void testFatfreecrm154() {
    final String appName = "fatfreecrm";
    final int stmtId = 154;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `contacts` AS `contacts` WHERE (`contacts`.`first_name` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts`.`last_name` LIKE '%page_sanford@kuvalisraynor.biz%' OR (`contacts`.`email` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts`.`alt_email` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts`.`phone` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts`.`mobile` LIKE '%page_sanford@kuvalisraynor.biz%')) AND (`contacts`.`assigned_to` = 547 OR (`contacts`.`user_id` = 547 OR `contacts`.`access` = 'Public'))"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 94
  void testFatfreecrm175() {
    final String appName = "fatfreecrm";
    final int stmtId = 175;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `leads` AS `leads` WHERE `leads`.`assigned_to` = 1124 OR (`leads`.`user_id` = 1124 OR `leads`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 95
  void testFatfreecrm176() {
    final String appName = "fatfreecrm";
    final int stmtId = 176;
    final String[] expected =
        new String[] {
          "SELECT 1 AS `one` FROM `taggings` AS `taggings` WHERE `taggings`.`taggable_type` = 'Lead' AND `taggings`.`taggable_id` = 511 AND `taggings`.`context` = 'tags' LIMIT 1"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 96
  void testFatfreecrm178() {
    final String appName = "fatfreecrm";
    final int stmtId = 178;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE `opportunities`.`stage` IN (?) AND (`opportunities`.`assigned_to` = 1503 OR (`opportunities`.`user_id` = 1503 OR `opportunities`.`access` = 'Public'))"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 97
  void testFatfreecrm181() {
    final String appName = "fatfreecrm";
    final int stmtId = 181;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `contacts` AS `contacts` WHERE `contacts`.`assigned_to` = 865 OR (`contacts`.`user_id` = 865 OR `contacts`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 98
  void testFatfreecrm187() {
    final String appName = "fatfreecrm";
    final int stmtId = 187;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `campaigns` AS `campaigns` WHERE `campaigns`.`assigned_to` = 541 OR (`campaigns`.`user_id` = 541 OR `campaigns`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 99
  void testFatfreecrm190() {
    final String appName = "fatfreecrm";
    final int stmtId = 190;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE `opportunities`.`name` LIKE '%second%' AND (`opportunities`.`assigned_to` = 1143 OR (`opportunities`.`user_id` = 1143 OR `opportunities`.`access` = 'Public'))"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 100
  void testFatfreecrm192() {
    final String appName = "fatfreecrm";
    final int stmtId = 192;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `leads` AS `leads` WHERE (`leads`.`first_name` LIKE '%bill%' OR `leads`.`last_name` LIKE '%bill%' OR `leads`.`company` LIKE '%bill%' OR `leads`.`email` LIKE '%bill%') AND (`leads`.`assigned_to` = 879 OR (`leads`.`user_id` = 879 OR `leads`.`access` = 'Public'))"
        };
    doTest(appName, stmtId, expected);
  }

  // 101
  // TODO: febs-52 aggregate
  // 102
  // TODO: febs-94 aggregate
  // 103
  // TODO: febs-96 aggregate
  // 104
  // TODO: febs-98 aggregate
  // 105
  // TODO: febs-101 aggregate

  @Test // 106
  void testHomeland12() {
    final String appName = "homeland";
    final int stmtId = 12;
    final String[] expected =
        new String[] {
          "SELECT \"actions\".\"user_id\" AS \"user_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"action_type\" = $2 AND \"actions\".\"target_id\" = $1 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"user_type\" = $4 AND \"actions\".\"user_type\" = $5",
          "SELECT \"actions\".\"user_id\" AS \"user_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"action_type\" = $2 AND \"actions\".\"target_id\" = $1 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"user_type\" = $5 AND \"actions\".\"user_type\" = $4",
          "SELECT \"actions\".\"user_id\" AS \"user_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"user_type\" = $5 AND \"actions\".\"user_type\" = $4 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"target_id\" = $1 AND \"actions\".\"action_type\" = $2"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 107
  void testHomeland31() {
    final String appName = "homeland";
    final int stmtId = 31;
    final String[] expected =
        new String[] {
          "SELECT COUNT(\"subquery_for_count\".\"count_column\") FROM (SELECT \"users\".\"type\" AS \"type\", \"users\".\"id\" AS \"id\", \"users\".\"name\" AS \"name\", \"users\".\"login\" AS \"login\", \"users\".\"email\" AS \"email\", \"users\".\"email_md5\" AS \"email_md5\", \"users\".\"email_public\" AS \"email_public\", \"users\".\"avatar\" AS \"avatar\", \"users\".\"state\" AS \"state\", \"users\".\"tagline\" AS \"tagline\", \"users\".\"github\" AS \"github\", \"users\".\"website\" AS \"website\", \"users\".\"location\" AS \"location\", \"users\".\"location_id\" AS \"location_id\", \"users\".\"twitter\" AS \"twitter\", \"users\".\"team_users_count\" AS \"team_users_count\", \"users\".\"created_at\" AS \"created_at\", \"users\".\"updated_at\" AS \"count_column\" FROM \"users\" AS \"users\" WHERE NOT \"users\".\"type\" IS NULL AND \"users\".\"location_id\" = 1 LIMIT 25 OFFSET 0) AS \"subquery_for_count\""
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 108
  void testHomeland72() {
    final String appName = "homeland";
    final int stmtId = 72;
    final String[] expected =
        new String[] {
          "SELECT \"actions\".\"target_id\" AS \"target_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"action_type\" = $2 AND \"actions\".\"target_type\" = $5 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"user_id\" = $1 AND \"actions\".\"user_type\" = $4",
          "SELECT \"actions\".\"target_id\" AS \"target_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"action_type\" = $2 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"target_type\" = $5 AND \"actions\".\"user_id\" = $1 AND \"actions\".\"user_type\" = $4",
          "SELECT \"actions\".\"target_id\" AS \"target_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"user_type\" = $4 AND \"actions\".\"user_id\" = $1 AND \"actions\".\"target_type\" = $5 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"action_type\" = $2"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 109
  void testHomeland73() {
    final String appName = "homeland";
    final int stmtId = 73;
    // TODO!
    final String[] expected =
        new String[] {
          "SELECT \"actions\".\"target_id\" AS \"target_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"action_type\" = $2 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"target_type\" = $5 AND \"actions\".\"user_id\" = $1 AND \"actions\".\"user_type\" = $4",
          "SELECT \"actions\".\"target_id\" AS \"target_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"action_type\" = $2 AND \"actions\".\"target_type\" = $5 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"user_id\" = $1 AND \"actions\".\"user_type\" = $4",
          "SELECT \"actions\".\"target_id\" AS \"target_id\" FROM \"actions\" AS \"actions\" WHERE \"actions\".\"user_type\" = $4 AND \"actions\".\"user_id\" = $1 AND \"actions\".\"target_type\" = $5 AND \"actions\".\"target_type\" = $3 AND \"actions\".\"action_type\" = $2"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 110
  void testLobsters93() {
    final String appName = "lobsters";
    final int stmtId = 93;
    final String[] expected =
        new String[] {
          "SELECT COUNT(`comments`.`id`) FROM `comments` AS `comments` WHERE `comments`.`is_deleted` = FALSE AND `comments`.`is_moderated` = FALSE AND `comments`.`story_id` = 67",
          "SELECT COUNT(`comments`.`id`) FROM `comments` AS `comments` WHERE `comments`.`story_id` = 67 AND `comments`.`is_moderated` = FALSE AND `comments`.`is_deleted` = FALSE",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 111
  void testLobsters95() {
    final String appName = "lobsters";
    final int stmtId = 95;
    final String[] expected =
        new String[] {
          "SELECT COUNT(DISTINCT `comments`.`id`) FROM `comments` AS `comments` INNER JOIN `stories` AS `stories` ON `comments`.`story_id` = `stories`.`id` INNER JOIN `domains` AS `domains` ON `stories`.`domain_id` = `domains`.`id` WHERE MATCH `comments`.`comment` AGAINST ('comment3 comment4' IN BOOLEAN MODE) AND `comments`.`is_deleted` = FALSE AND `comments`.`is_moderated` = FALSE AND `domains`.`domain` = 'lobste.rs'",
          "SELECT COUNT(DISTINCT `comments`.`id`) FROM `comments` AS `comments` INNER JOIN `stories` AS `stories` ON `comments`.`story_id` = `stories`.`id` INNER JOIN `domains` AS `domains` ON `stories`.`domain_id` = `domains`.`id` WHERE `domains`.`domain` = 'lobste.rs' AND `comments`.`is_moderated` = FALSE AND `comments`.`is_deleted` = FALSE AND MATCH `comments`.`comment` AGAINST ('comment3 comment4' IN BOOLEAN MODE)"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 112
  void testLobsters114() {
    final String appName = "lobsters";
    final int stmtId = 114;
    final String[] expected =
        new String[] {
          "SELECT COUNT(`comments`.`id`) FROM `comments` AS `comments` WHERE MATCH `comments`.`comment` AGAINST ('comment1' IN BOOLEAN MODE) AND `comments`.`is_deleted` = FALSE AND `comments`.`is_moderated` = FALSE",
          "SELECT COUNT(`comments`.`id`) FROM `comments` AS `comments` WHERE `comments`.`is_moderated` = FALSE AND `comments`.`is_deleted` = FALSE AND MATCH `comments`.`comment` AGAINST ('comment1' IN BOOLEAN MODE)"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 113
  void testLobsters129() {
    final String appName = "lobsters";
    final int stmtId = 129;
    final String[] expected =
        new String[] {
          "SELECT COUNT(`stories`.`id`) FROM `stories` AS `stories` WHERE (MATCH `stories`.`title` AGAINST ('unique' IN BOOLEAN MODE) OR MATCH `stories`.`description` AGAINST ('unique' IN BOOLEAN MODE) OR MATCH `stories`.`story_cache` AGAINST ('unique' IN BOOLEAN MODE)) AND `stories`.`merged_story_id` IS NULL AND `stories`.`is_expired` = FALSE"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 114
  void testPybbs31() {
    final String appName = "pybbs";
    final int stmtId = 31;
    final String[] expected =
        new String[] {
          "SELECT COUNT(1) FROM `topic` AS `t`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 115
  void testPybbs33() {
    final String appName = "pybbs";
    final int stmtId = 33;
    final String[] expected =
        new String[] {
          "SELECT COUNT(1) FROM `topic` AS `t` WHERE `t`.`in_time` BETWEEN '2019-10-11' AND '2019-10-26'",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 116
  void testPybbs51() {
    final String appName = "pybbs";
    final int stmtId = 51;
    final String[] expected =
        new String[] {
          "SELECT COUNT(1) FROM `comment` AS `c`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 117
  void testPybbs53() {
    final String appName = "pybbs";
    final int stmtId = 53;
    final String[] expected =
        new String[] {
          "SELECT COUNT(1) FROM `comment` AS `c` WHERE `c`.`in_time` BETWEEN '2019-10-01' AND '2019-10-31'",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 118
  void testRedmine136() {
    final String appName = "redmine";
    final int stmtId = 136;
    final String[] expected =
        new String[] {
          "SELECT DISTINCT `users`.`id` AS `id`, `users`.`login` AS `login`, `users`.`hashed_password` AS `hashed_password`, `users`.`firstname` AS `firstname`, `users`.`lastname` AS `lastname`, `users`.`admin` AS `admin`, `users`.`status` AS `status`, `users`.`last_login_on` AS `last_login_on`, `users`.`language` AS `language`, `users`.`auth_source_id` AS `auth_source_id`, `users`.`created_on` AS `created_on`, `users`.`updated_on` AS `updated_on`, `users`.`type` AS `type`, `users`.`identity_url` AS `identity_url`, `users`.`mail_notification` AS `mail_notification`, `users`.`salt` AS `salt`, `users`.`must_change_passwd` AS `must_change_passwd`, `users`.`passwd_changed_on` AS `passwd_changed_on` FROM `users` AS `users` INNER JOIN `email_addresses` AS `email_addresses` ON `users`.`id` = `email_addresses`.`user_id` WHERE `users`.`type` IN (?) AND LOWER(`email_addresses`.`address`) IN (?) ORDER BY `users`.`id` ASC LIMIT 1"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 119
  void testRedmine284() {
    final String appName = "redmine";
    final int stmtId = 284;
    final String[] expected =
        new String[] {
          "SELECT 1 AS `one` FROM `projects_trackers` AS `projects_trackers` WHERE `projects_trackers`.`project_id` = 1 LIMIT 1",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 120
  void testRedmine341() {
    final String appName = "redmine";
    final int stmtId = 341;
    final String[] expected =
        new String[] {
          "SELECT `changesets_issues`.`issue_id` AS `issue_id` FROM `changesets_issues` AS `changesets_issues` WHERE `changesets_issues`.`changeset_id` = 339",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 121
  void testRedmine535() {
    final String appName = "redmine";
    final int stmtId = 535;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `watchers` AS `watchers` WHERE `watchers`.`watchable_id` = 1 AND `watchers`.`watchable_type` = 'Issue'",
          "SELECT COUNT(*) FROM `watchers` AS `watchers` WHERE `watchers`.`watchable_type` = 'Issue' AND `watchers`.`watchable_id` = 1"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 122
  void testShopizer1() {
    final String appName = "shopizer";
    final int stmtId = 1;
    final String[] expected =
        new String[] {
          "SELECT `productopt0_`.`product_option_id` AS `product_1_50_0_`, `merchantst1_`.`merchant_id` AS `merchant1_27_1_`, `descriptio2_`.`description_id` AS `descript1_51_2_`, `productopt0_`.`product_option_code` AS `product_2_50_0_`, `productopt0_`.`merchant_id` AS `merchant6_50_0_`, `productopt0_`.`product_option_sort_ord` AS `product_3_50_0_`, `productopt0_`.`product_option_type` AS `product_4_50_0_`, `productopt0_`.`product_option_read` AS `product_5_50_0_`, `merchantst1_`.`date_created` AS `date_cre2_27_1_`, `merchantst1_`.`date_modified` AS `date_mod3_27_1_`, `merchantst1_`.`updt_id` AS `updt_id4_27_1_`, `merchantst1_`.`store_code` AS `store_co5_27_1_`, `merchantst1_`.`continueshoppingurl` AS `continue6_27_1_`, `merchantst1_`.`country_id` AS `country23_27_1_`, `merchantst1_`.`currency_id` AS `currenc24_27_1_`, `merchantst1_`.`currency_format_national` AS `currency7_27_1_`, `merchantst1_`.`language_id` AS `languag25_27_1_`, `merchantst1_`.`domain_name` AS `domain_n8_27_1_`, `merchantst1_`.`in_business_since` AS `in_busin9_27_1_`, `merchantst1_`.`invoice_template` AS `invoice10_27_1_`, `merchantst1_`.`seizeunitcode` AS `seizeun11_27_1_`, `merchantst1_`.`store_email` AS `store_e12_27_1_`, `merchantst1_`.`store_logo` AS `store_l13_27_1_`, `merchantst1_`.`store_template` AS `store_t14_27_1_`, `merchantst1_`.`store_address` AS `store_a15_27_1_`, `merchantst1_`.`store_city` AS `store_c16_27_1_`, `merchantst1_`.`store_name` AS `store_n17_27_1_`, `merchantst1_`.`store_phone` AS `store_p18_27_1_`, `merchantst1_`.`store_postal_code` AS `store_p19_27_1_`, `merchantst1_`.`store_state_prov` AS `store_s20_27_1_`, `merchantst1_`.`use_cache` AS `use_cac21_27_1_`, `merchantst1_`.`weightunitcode` AS `weightu22_27_1_`, `merchantst1_`.`zone_id` AS `zone_id26_27_1_`, `descriptio2_`.`date_created` AS `date_cre2_51_2_`, `descriptio2_`.`date_modified` AS `date_mod3_51_2_`, `descriptio2_`.`updt_id` AS `updt_id4_51_2_`, `descriptio2_`.`description` AS `descript5_51_2_`, `descriptio2_`.`language_id` AS `language9_51_2_`, `descriptio2_`.`name` AS `name6_51_2_`, `descriptio2_`.`title` AS `title7_51_2_`, `descriptio2_`.`product_option_id` AS `product10_51_2_`, `descriptio2_`.`product_option_comment` AS `product_8_51_2_`, `descriptio2_`.`product_option_id` AS `product10_51_0__`, `descriptio2_`.`description_id` AS `descript1_51_0__` FROM `product_option` AS `productopt0_` INNER JOIN `merchant_store` AS `merchantst1_` ON `productopt0_`.`merchant_id` = `merchantst1_`.`merchant_id` INNER JOIN `product_option_desc` AS `descriptio2_` ON `productopt0_`.`product_option_id` = `descriptio2_`.`product_option_id` WHERE `merchantst1_`.`merchant_id` = 1 AND `descriptio2_`.`language_id` = 1"
        };
    doTest(appName, stmtId, expected);
  }

  // 123 shopizer-3 slow

  @Test // 124
  void testShopizer14() {
    final String appName = "shopizer";
    final int stmtId = 14;
    final String[] expected =
        new String[] {
          "SELECT `category0_`.`category_id` AS `category1_0_0_`, `descriptio1_`.`description_id` AS `descript1_1_1_`, `language2_`.`language_id` AS `language1_21_2_`, `merchantst3_`.`merchant_id` AS `merchant1_27_3_`, `category0_`.`date_created` AS `date_cre2_0_0_`, `category0_`.`date_modified` AS `date_mod3_0_0_`, `category0_`.`updt_id` AS `updt_id4_0_0_`, `category0_`.`category_image` AS `category5_0_0_`, `category0_`.`category_status` AS `category6_0_0_`, `category0_`.`code` AS `code7_0_0_`, `category0_`.`depth` AS `depth8_0_0_`, `category0_`.`featured` AS `featured9_0_0_`, `category0_`.`lineage` AS `lineage10_0_0_`, `category0_`.`merchant_id` AS `merchan13_0_0_`, `category0_`.`parent_id` AS `parent_14_0_0_`, `category0_`.`sort_order` AS `sort_or11_0_0_`, `category0_`.`visible` AS `visible12_0_0_`, `descriptio1_`.`date_created` AS `date_cre2_1_1_`, `descriptio1_`.`date_modified` AS `date_mod3_1_1_`, `descriptio1_`.`updt_id` AS `updt_id4_1_1_`, `descriptio1_`.`description` AS `descript5_1_1_`, `descriptio1_`.`language_id` AS `languag13_1_1_`, `descriptio1_`.`name` AS `name6_1_1_`, `descriptio1_`.`title` AS `title7_1_1_`, `descriptio1_`.`category_id` AS `categor14_1_1_`, `descriptio1_`.`category_highlight` AS `category8_1_1_`, `descriptio1_`.`meta_description` AS `meta_des9_1_1_`, `descriptio1_`.`meta_keywords` AS `meta_ke10_1_1_`, `descriptio1_`.`meta_title` AS `meta_ti11_1_1_`, `descriptio1_`.`sef_url` AS `sef_url12_1_1_`, `descriptio1_`.`category_id` AS `categor14_1_0__`, `descriptio1_`.`description_id` AS `descript1_1_0__`, `language2_`.`date_created` AS `date_cre2_21_2_`, `language2_`.`date_modified` AS `date_mod3_21_2_`, `language2_`.`updt_id` AS `updt_id4_21_2_`, `language2_`.`code` AS `code5_21_2_`, `language2_`.`sort_order` AS `sort_ord6_21_2_`, `merchantst3_`.`date_created` AS `date_cre2_27_3_`, `merchantst3_`.`date_modified` AS `date_mod3_27_3_`, `merchantst3_`.`updt_id` AS `updt_id4_27_3_`, `merchantst3_`.`store_code` AS `store_co5_27_3_`, `merchantst3_`.`continueshoppingurl` AS `continue6_27_3_`, `merchantst3_`.`country_id` AS `country23_27_3_`, `merchantst3_`.`currency_id` AS `currenc24_27_3_`, `merchantst3_`.`currency_format_national` AS `currency7_27_3_`, `merchantst3_`.`language_id` AS `languag25_27_3_`, `merchantst3_`.`domain_name` AS `domain_n8_27_3_`, `merchantst3_`.`in_business_since` AS `in_busin9_27_3_`, `merchantst3_`.`invoice_template` AS `invoice10_27_3_`, `merchantst3_`.`seizeunitcode` AS `seizeun11_27_3_`, `merchantst3_`.`store_email` AS `store_e12_27_3_`, `merchantst3_`.`store_logo` AS `store_l13_27_3_`, `merchantst3_`.`store_template` AS `store_t14_27_3_`, `merchantst3_`.`store_address` AS `store_a15_27_3_`, `merchantst3_`.`store_city` AS `store_c16_27_3_`, `merchantst3_`.`store_name` AS `store_n17_27_3_`, `merchantst3_`.`store_phone` AS `store_p18_27_3_`, `merchantst3_`.`store_postal_code` AS `store_p19_27_3_`, `merchantst3_`.`store_state_prov` AS `store_s20_27_3_`, `merchantst3_`.`use_cache` AS `use_cac21_27_3_`, `merchantst3_`.`weightunitcode` AS `weightu22_27_3_`, `merchantst3_`.`zone_id` AS `zone_id26_27_3_` FROM `category` AS `category0_` INNER JOIN `category_description` AS `descriptio1_` ON `category0_`.`category_id` = `descriptio1_`.`category_id` INNER JOIN `merchant_store` AS `merchantst3_` ON `category0_`.`merchant_id` = `merchantst3_`.`merchant_id` INNER JOIN `language` AS `language2_` ON `descriptio1_`.`language_id` = `language2_`.`language_id` WHERE `language2_`.`language_id` = 1 AND `merchantst3_`.`merchant_id` = 1 AND `category0_`.`depth` >= 0 ORDER BY `category0_`.`lineage`, `category0_`.`sort_order` ASC"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 125
  void testShopizer18() {
    final String appName = "shopizer";
    final int stmtId = 18;
    final String[] expected =
        new String[] {
          "SELECT `category0_`.`category_id` AS `category1_0_0_`, `descriptio1_`.`description_id` AS `descript1_1_1_`, `language2_`.`language_id` AS `language1_21_2_`, `merchantst3_`.`merchant_id` AS `merchant1_27_3_`, `category0_`.`date_created` AS `date_cre2_0_0_`, `category0_`.`date_modified` AS `date_mod3_0_0_`, `category0_`.`updt_id` AS `updt_id4_0_0_`, `category0_`.`category_image` AS `category5_0_0_`, `category0_`.`category_status` AS `category6_0_0_`, `category0_`.`code` AS `code7_0_0_`, `category0_`.`depth` AS `depth8_0_0_`, `category0_`.`featured` AS `featured9_0_0_`, `category0_`.`lineage` AS `lineage10_0_0_`, `category0_`.`merchant_id` AS `merchan13_0_0_`, `category0_`.`parent_id` AS `parent_14_0_0_`, `category0_`.`sort_order` AS `sort_or11_0_0_`, `category0_`.`visible` AS `visible12_0_0_`, `descriptio1_`.`date_created` AS `date_cre2_1_1_`, `descriptio1_`.`date_modified` AS `date_mod3_1_1_`, `descriptio1_`.`updt_id` AS `updt_id4_1_1_`, `descriptio1_`.`description` AS `descript5_1_1_`, `descriptio1_`.`language_id` AS `languag13_1_1_`, `descriptio1_`.`name` AS `name6_1_1_`, `descriptio1_`.`title` AS `title7_1_1_`, `descriptio1_`.`category_id` AS `categor14_1_1_`, `descriptio1_`.`category_highlight` AS `category8_1_1_`, `descriptio1_`.`meta_description` AS `meta_des9_1_1_`, `descriptio1_`.`meta_keywords` AS `meta_ke10_1_1_`, `descriptio1_`.`meta_title` AS `meta_ti11_1_1_`, `descriptio1_`.`sef_url` AS `sef_url12_1_1_`, `descriptio1_`.`category_id` AS `categor14_1_0__`, `descriptio1_`.`description_id` AS `descript1_1_0__`, `language2_`.`date_created` AS `date_cre2_21_2_`, `language2_`.`date_modified` AS `date_mod3_21_2_`, `language2_`.`updt_id` AS `updt_id4_21_2_`, `language2_`.`code` AS `code5_21_2_`, `language2_`.`sort_order` AS `sort_ord6_21_2_`, `merchantst3_`.`date_created` AS `date_cre2_27_3_`, `merchantst3_`.`date_modified` AS `date_mod3_27_3_`, `merchantst3_`.`updt_id` AS `updt_id4_27_3_`, `merchantst3_`.`store_code` AS `store_co5_27_3_`, `merchantst3_`.`continueshoppingurl` AS `continue6_27_3_`, `merchantst3_`.`country_id` AS `country23_27_3_`, `merchantst3_`.`currency_id` AS `currenc24_27_3_`, `merchantst3_`.`currency_format_national` AS `currency7_27_3_`, `merchantst3_`.`language_id` AS `languag25_27_3_`, `merchantst3_`.`domain_name` AS `domain_n8_27_3_`, `merchantst3_`.`in_business_since` AS `in_busin9_27_3_`, `merchantst3_`.`invoice_template` AS `invoice10_27_3_`, `merchantst3_`.`seizeunitcode` AS `seizeun11_27_3_`, `merchantst3_`.`store_email` AS `store_e12_27_3_`, `merchantst3_`.`store_logo` AS `store_l13_27_3_`, `merchantst3_`.`store_template` AS `store_t14_27_3_`, `merchantst3_`.`store_address` AS `store_a15_27_3_`, `merchantst3_`.`store_city` AS `store_c16_27_3_`, `merchantst3_`.`store_name` AS `store_n17_27_3_`, `merchantst3_`.`store_phone` AS `store_p18_27_3_`, `merchantst3_`.`store_postal_code` AS `store_p19_27_3_`, `merchantst3_`.`store_state_prov` AS `store_s20_27_3_`, `merchantst3_`.`use_cache` AS `use_cac21_27_3_`, `merchantst3_`.`weightunitcode` AS `weightu22_27_3_`, `merchantst3_`.`zone_id` AS `zone_id26_27_3_` FROM `category` AS `category0_` INNER JOIN `category_description` AS `descriptio1_` ON `category0_`.`category_id` = `descriptio1_`.`category_id` INNER JOIN `language` AS `language2_` ON `descriptio1_`.`language_id` = `language2_`.`language_id` INNER JOIN `merchant_store` AS `merchantst3_` ON `category0_`.`merchant_id` = `merchantst3_`.`merchant_id` WHERE `merchantst3_`.`merchant_id` = 1 AND `category0_`.`lineage` LIKE '%/1/%' ORDER BY `category0_`.`lineage`, `category0_`.`sort_order` ASC"
        };
    doTest(appName, stmtId, expected);
  }

  // 126 shopizer-24 slow

  @Test // 127
  void testShopizer31() {
    final String appName = "shopizer";
    final int stmtId = 31;
    final String[] expected =
        new String[] {
          "SELECT `category0_`.`category_id` AS `category1_0_0_`, `descriptio1_`.`description_id` AS `descript1_1_1_`, `language2_`.`language_id` AS `language1_21_2_`, `merchantst3_`.`merchant_id` AS `merchant1_27_3_`, `category0_`.`date_created` AS `date_cre2_0_0_`, `category0_`.`date_modified` AS `date_mod3_0_0_`, `category0_`.`updt_id` AS `updt_id4_0_0_`, `category0_`.`category_image` AS `category5_0_0_`, `category0_`.`category_status` AS `category6_0_0_`, `category0_`.`code` AS `code7_0_0_`, `category0_`.`depth` AS `depth8_0_0_`, `category0_`.`featured` AS `featured9_0_0_`, `category0_`.`lineage` AS `lineage10_0_0_`, `category0_`.`merchant_id` AS `merchan13_0_0_`, `category0_`.`parent_id` AS `parent_14_0_0_`, `category0_`.`sort_order` AS `sort_or11_0_0_`, `category0_`.`visible` AS `visible12_0_0_`, `descriptio1_`.`date_created` AS `date_cre2_1_1_`, `descriptio1_`.`date_modified` AS `date_mod3_1_1_`, `descriptio1_`.`updt_id` AS `updt_id4_1_1_`, `descriptio1_`.`description` AS `descript5_1_1_`, `descriptio1_`.`language_id` AS `languag13_1_1_`, `descriptio1_`.`name` AS `name6_1_1_`, `descriptio1_`.`title` AS `title7_1_1_`, `descriptio1_`.`category_id` AS `categor14_1_1_`, `descriptio1_`.`category_highlight` AS `category8_1_1_`, `descriptio1_`.`meta_description` AS `meta_des9_1_1_`, `descriptio1_`.`meta_keywords` AS `meta_ke10_1_1_`, `descriptio1_`.`meta_title` AS `meta_ti11_1_1_`, `descriptio1_`.`sef_url` AS `sef_url12_1_1_`, `descriptio1_`.`category_id` AS `categor14_1_0__`, `descriptio1_`.`description_id` AS `descript1_1_0__`, `language2_`.`date_created` AS `date_cre2_21_2_`, `language2_`.`date_modified` AS `date_mod3_21_2_`, `language2_`.`updt_id` AS `updt_id4_21_2_`, `language2_`.`code` AS `code5_21_2_`, `language2_`.`sort_order` AS `sort_ord6_21_2_`, `merchantst3_`.`date_created` AS `date_cre2_27_3_`, `merchantst3_`.`date_modified` AS `date_mod3_27_3_`, `merchantst3_`.`updt_id` AS `updt_id4_27_3_`, `merchantst3_`.`store_code` AS `store_co5_27_3_`, `merchantst3_`.`continueshoppingurl` AS `continue6_27_3_`, `merchantst3_`.`country_id` AS `country23_27_3_`, `merchantst3_`.`currency_id` AS `currenc24_27_3_`, `merchantst3_`.`currency_format_national` AS `currency7_27_3_`, `merchantst3_`.`language_id` AS `languag25_27_3_`, `merchantst3_`.`domain_name` AS `domain_n8_27_3_`, `merchantst3_`.`in_business_since` AS `in_busin9_27_3_`, `merchantst3_`.`invoice_template` AS `invoice10_27_3_`, `merchantst3_`.`seizeunitcode` AS `seizeun11_27_3_`, `merchantst3_`.`store_email` AS `store_e12_27_3_`, `merchantst3_`.`store_logo` AS `store_l13_27_3_`, `merchantst3_`.`store_template` AS `store_t14_27_3_`, `merchantst3_`.`store_address` AS `store_a15_27_3_`, `merchantst3_`.`store_city` AS `store_c16_27_3_`, `merchantst3_`.`store_name` AS `store_n17_27_3_`, `merchantst3_`.`store_phone` AS `store_p18_27_3_`, `merchantst3_`.`store_postal_code` AS `store_p19_27_3_`, `merchantst3_`.`store_state_prov` AS `store_s20_27_3_`, `merchantst3_`.`use_cache` AS `use_cac21_27_3_`, `merchantst3_`.`weightunitcode` AS `weightu22_27_3_`, `merchantst3_`.`zone_id` AS `zone_id26_27_3_` FROM `category` AS `category0_` INNER JOIN `category_description` AS `descriptio1_` ON `category0_`.`category_id` = `descriptio1_`.`category_id` INNER JOIN `merchant_store` AS `merchantst3_` ON `category0_`.`merchant_id` = `merchantst3_`.`merchant_id` INNER JOIN `language` AS `language2_` ON `descriptio1_`.`language_id` = `language2_`.`language_id` WHERE `language2_`.`language_id` = 1 AND `merchantst3_`.`merchant_id` = 1 ORDER BY `category0_`.`lineage`, `category0_`.`sort_order` ASC"
        };
    doTest(appName, stmtId, expected);
  }

  // 128 shopizer-39 slow

  @Test // 129
  void testShopizer40() {
    final String appName = "shopizer";
    final int stmtId = 40;
    final String[] expected =
        new String[] {
          "SELECT `category0_`.`category_id` AS `category1_0_0_`, `descriptio1_`.`description_id` AS `descript1_1_1_`, `language2_`.`language_id` AS `language1_21_2_`, `merchantst3_`.`merchant_id` AS `merchant1_27_3_`, `category0_`.`date_created` AS `date_cre2_0_0_`, `category0_`.`date_modified` AS `date_mod3_0_0_`, `category0_`.`updt_id` AS `updt_id4_0_0_`, `category0_`.`category_image` AS `category5_0_0_`, `category0_`.`category_status` AS `category6_0_0_`, `category0_`.`code` AS `code7_0_0_`, `category0_`.`depth` AS `depth8_0_0_`, `category0_`.`featured` AS `featured9_0_0_`, `category0_`.`lineage` AS `lineage10_0_0_`, `category0_`.`merchant_id` AS `merchan13_0_0_`, `category0_`.`parent_id` AS `parent_14_0_0_`, `category0_`.`sort_order` AS `sort_or11_0_0_`, `category0_`.`visible` AS `visible12_0_0_`, `descriptio1_`.`date_created` AS `date_cre2_1_1_`, `descriptio1_`.`date_modified` AS `date_mod3_1_1_`, `descriptio1_`.`updt_id` AS `updt_id4_1_1_`, `descriptio1_`.`description` AS `descript5_1_1_`, `descriptio1_`.`language_id` AS `languag13_1_1_`, `descriptio1_`.`name` AS `name6_1_1_`, `descriptio1_`.`title` AS `title7_1_1_`, `descriptio1_`.`category_id` AS `categor14_1_1_`, `descriptio1_`.`category_highlight` AS `category8_1_1_`, `descriptio1_`.`meta_description` AS `meta_des9_1_1_`, `descriptio1_`.`meta_keywords` AS `meta_ke10_1_1_`, `descriptio1_`.`meta_title` AS `meta_ti11_1_1_`, `descriptio1_`.`sef_url` AS `sef_url12_1_1_`, `descriptio1_`.`category_id` AS `categor14_1_0__`, `descriptio1_`.`description_id` AS `descript1_1_0__`, `language2_`.`date_created` AS `date_cre2_21_2_`, `language2_`.`date_modified` AS `date_mod3_21_2_`, `language2_`.`updt_id` AS `updt_id4_21_2_`, `language2_`.`code` AS `code5_21_2_`, `language2_`.`sort_order` AS `sort_ord6_21_2_`, `merchantst3_`.`date_created` AS `date_cre2_27_3_`, `merchantst3_`.`date_modified` AS `date_mod3_27_3_`, `merchantst3_`.`updt_id` AS `updt_id4_27_3_`, `merchantst3_`.`store_code` AS `store_co5_27_3_`, `merchantst3_`.`continueshoppingurl` AS `continue6_27_3_`, `merchantst3_`.`country_id` AS `country23_27_3_`, `merchantst3_`.`currency_id` AS `currenc24_27_3_`, `merchantst3_`.`currency_format_national` AS `currency7_27_3_`, `merchantst3_`.`language_id` AS `languag25_27_3_`, `merchantst3_`.`domain_name` AS `domain_n8_27_3_`, `merchantst3_`.`in_business_since` AS `in_busin9_27_3_`, `merchantst3_`.`invoice_template` AS `invoice10_27_3_`, `merchantst3_`.`seizeunitcode` AS `seizeun11_27_3_`, `merchantst3_`.`store_email` AS `store_e12_27_3_`, `merchantst3_`.`store_logo` AS `store_l13_27_3_`, `merchantst3_`.`store_template` AS `store_t14_27_3_`, `merchantst3_`.`store_address` AS `store_a15_27_3_`, `merchantst3_`.`store_city` AS `store_c16_27_3_`, `merchantst3_`.`store_name` AS `store_n17_27_3_`, `merchantst3_`.`store_phone` AS `store_p18_27_3_`, `merchantst3_`.`store_postal_code` AS `store_p19_27_3_`, `merchantst3_`.`store_state_prov` AS `store_s20_27_3_`, `merchantst3_`.`use_cache` AS `use_cac21_27_3_`, `merchantst3_`.`weightunitcode` AS `weightu22_27_3_`, `merchantst3_`.`zone_id` AS `zone_id26_27_3_` FROM `category` AS `category0_` INNER JOIN `category_description` AS `descriptio1_` ON `category0_`.`category_id` = `descriptio1_`.`category_id` INNER JOIN `language` AS `language2_` ON `descriptio1_`.`language_id` = `language2_`.`language_id` INNER JOIN `merchant_store` AS `merchantst3_` ON `category0_`.`merchant_id` = `merchantst3_`.`merchant_id` WHERE `merchantst3_`.`store_code` = 'DEFAULT' AND `category0_`.`lineage` LIKE '%/2/%' ORDER BY `category0_`.`lineage`, `category0_`.`sort_order` ASC"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 130
  void testShopizer46() {
    final String appName = "shopizer";
    final int stmtId = 46;
    final String[] expected =
        new String[] {
          "SELECT `productopt0_`.`product_option_value_id` AS `product_1_52_0_`, `merchantst1_`.`merchant_id` AS `merchant1_27_1_`, `descriptio2_`.`description_id` AS `descript1_53_2_`, `productopt0_`.`product_option_val_code` AS `product_2_52_0_`, `productopt0_`.`merchant_id` AS `merchant6_52_0_`, `productopt0_`.`product_opt_for_disp` AS `product_3_52_0_`, `productopt0_`.`product_opt_val_image` AS `product_4_52_0_`, `productopt0_`.`product_opt_val_sort_ord` AS `product_5_52_0_`, `merchantst1_`.`date_created` AS `date_cre2_27_1_`, `merchantst1_`.`date_modified` AS `date_mod3_27_1_`, `merchantst1_`.`updt_id` AS `updt_id4_27_1_`, `merchantst1_`.`store_code` AS `store_co5_27_1_`, `merchantst1_`.`continueshoppingurl` AS `continue6_27_1_`, `merchantst1_`.`country_id` AS `country23_27_1_`, `merchantst1_`.`currency_id` AS `currenc24_27_1_`, `merchantst1_`.`currency_format_national` AS `currency7_27_1_`, `merchantst1_`.`language_id` AS `languag25_27_1_`, `merchantst1_`.`domain_name` AS `domain_n8_27_1_`, `merchantst1_`.`in_business_since` AS `in_busin9_27_1_`, `merchantst1_`.`invoice_template` AS `invoice10_27_1_`, `merchantst1_`.`seizeunitcode` AS `seizeun11_27_1_`, `merchantst1_`.`store_email` AS `store_e12_27_1_`, `merchantst1_`.`store_logo` AS `store_l13_27_1_`, `merchantst1_`.`store_template` AS `store_t14_27_1_`, `merchantst1_`.`store_address` AS `store_a15_27_1_`, `merchantst1_`.`store_city` AS `store_c16_27_1_`, `merchantst1_`.`store_name` AS `store_n17_27_1_`, `merchantst1_`.`store_phone` AS `store_p18_27_1_`, `merchantst1_`.`store_postal_code` AS `store_p19_27_1_`, `merchantst1_`.`store_state_prov` AS `store_s20_27_1_`, `merchantst1_`.`use_cache` AS `use_cac21_27_1_`, `merchantst1_`.`weightunitcode` AS `weightu22_27_1_`, `merchantst1_`.`zone_id` AS `zone_id26_27_1_`, `descriptio2_`.`date_created` AS `date_cre2_53_2_`, `descriptio2_`.`date_modified` AS `date_mod3_53_2_`, `descriptio2_`.`updt_id` AS `updt_id4_53_2_`, `descriptio2_`.`description` AS `descript5_53_2_`, `descriptio2_`.`language_id` AS `language8_53_2_`, `descriptio2_`.`name` AS `name6_53_2_`, `descriptio2_`.`title` AS `title7_53_2_`, `descriptio2_`.`product_option_value_id` AS `product_9_53_2_`, `descriptio2_`.`product_option_value_id` AS `product_9_53_0__`, `descriptio2_`.`description_id` AS `descript1_53_0__` FROM `product_option_value` AS `productopt0_` INNER JOIN `merchant_store` AS `merchantst1_` ON `productopt0_`.`merchant_id` = `merchantst1_`.`merchant_id` INNER JOIN `product_option_value_description` AS `descriptio2_` ON `productopt0_`.`product_option_value_id` = `descriptio2_`.`product_option_value_id` WHERE `productopt0_`.`merchant_id` = 1 AND `descriptio2_`.`language_id` = 1",
          "SELECT `productopt0_`.`product_option_value_id` AS `product_1_52_0_`, `merchantst1_`.`merchant_id` AS `merchant1_27_1_`, `descriptio2_`.`description_id` AS `descript1_53_2_`, `productopt0_`.`product_option_val_code` AS `product_2_52_0_`, `productopt0_`.`merchant_id` AS `merchant6_52_0_`, `productopt0_`.`product_opt_for_disp` AS `product_3_52_0_`, `productopt0_`.`product_opt_val_image` AS `product_4_52_0_`, `productopt0_`.`product_opt_val_sort_ord` AS `product_5_52_0_`, `merchantst1_`.`date_created` AS `date_cre2_27_1_`, `merchantst1_`.`date_modified` AS `date_mod3_27_1_`, `merchantst1_`.`updt_id` AS `updt_id4_27_1_`, `merchantst1_`.`store_code` AS `store_co5_27_1_`, `merchantst1_`.`continueshoppingurl` AS `continue6_27_1_`, `merchantst1_`.`country_id` AS `country23_27_1_`, `merchantst1_`.`currency_id` AS `currenc24_27_1_`, `merchantst1_`.`currency_format_national` AS `currency7_27_1_`, `merchantst1_`.`language_id` AS `languag25_27_1_`, `merchantst1_`.`domain_name` AS `domain_n8_27_1_`, `merchantst1_`.`in_business_since` AS `in_busin9_27_1_`, `merchantst1_`.`invoice_template` AS `invoice10_27_1_`, `merchantst1_`.`seizeunitcode` AS `seizeun11_27_1_`, `merchantst1_`.`store_email` AS `store_e12_27_1_`, `merchantst1_`.`store_logo` AS `store_l13_27_1_`, `merchantst1_`.`store_template` AS `store_t14_27_1_`, `merchantst1_`.`store_address` AS `store_a15_27_1_`, `merchantst1_`.`store_city` AS `store_c16_27_1_`, `merchantst1_`.`store_name` AS `store_n17_27_1_`, `merchantst1_`.`store_phone` AS `store_p18_27_1_`, `merchantst1_`.`store_postal_code` AS `store_p19_27_1_`, `merchantst1_`.`store_state_prov` AS `store_s20_27_1_`, `merchantst1_`.`use_cache` AS `use_cac21_27_1_`, `merchantst1_`.`weightunitcode` AS `weightu22_27_1_`, `merchantst1_`.`zone_id` AS `zone_id26_27_1_`, `descriptio2_`.`date_created` AS `date_cre2_53_2_`, `descriptio2_`.`date_modified` AS `date_mod3_53_2_`, `descriptio2_`.`updt_id` AS `updt_id4_53_2_`, `descriptio2_`.`description` AS `descript5_53_2_`, `descriptio2_`.`language_id` AS `language8_53_2_`, `descriptio2_`.`name` AS `name6_53_2_`, `descriptio2_`.`title` AS `title7_53_2_`, `descriptio2_`.`product_option_value_id` AS `product_9_53_2_`, `descriptio2_`.`product_option_value_id` AS `product_9_53_0__`, `descriptio2_`.`description_id` AS `descript1_53_0__` FROM `product_option_value` AS `productopt0_` INNER JOIN `merchant_store` AS `merchantst1_` ON `productopt0_`.`merchant_id` = `merchantst1_`.`merchant_id` INNER JOIN `product_option_value_description` AS `descriptio2_` ON `productopt0_`.`product_option_value_id` = `descriptio2_`.`product_option_value_id` WHERE `merchantst1_`.`merchant_id` = 1 AND `descriptio2_`.`language_id` = 1"
        };
    doTest(appName, stmtId, expected);
  }

  //  @Test // 132 // Slow
  void testShopizer67() {
    final String appName = "shopizer";
    final int stmtId = 67;
    final String[] expected =
        new String[] {
          "SELECT COUNT(DISTINCT `product0_`.`product_id`) AS `col_0_0_` FROM `product` AS `product0_` INNER JOIN `product_description` AS `descriptio1_` ON `product0_`.`product_id` = `descriptio1_`.`product_id` INNER JOIN `product_category` AS `categories2_` ON `product0_`.`product_id` = `categories2_`.`product_id` WHERE `descriptio1_`.`name` = 1 AND `descriptio1_`.`meta_title` = 1 AND `product0_`.`available` = 1 AND `product0_`.`date_available` <= '2019-10-21 21:17:32.7' AND `product0_`.`merchant_id` = 1 AND `descriptio1_`.`meta_keywords` IN (?)",
          "SELECT COUNT(DISTINCT `product0_`.`product_id`) AS `col_0_0_` FROM `product` AS `product0_` INNER JOIN `product_category` AS `categories2_` ON `product0_`.`product_id` = `categories2_`.`product_id` INNER JOIN `product_description` AS `descriptio1_` ON `product0_`.`product_id` = `descriptio1_`.`product_id` WHERE `descriptio1_`.`product_highlight` = 1 AND `product0_`.`available` = 1 AND `product0_`.`date_available` <= '2019-10-21 21:17:32.7' AND `product0_`.`merchant_id` = 1 AND `product0_`.`manufacturer_id` = 1 AND `descriptio1_`.`product_id` IN (?)",
          "SELECT COUNT(DISTINCT `product0_`.`product_id`) AS `col_0_0_` FROM `product` AS `product0_` INNER JOIN `product_description` AS `descriptio1_` ON `product0_`.`product_id` = `descriptio1_`.`product_id` INNER JOIN `product_category` AS `categories2_` ON `product0_`.`product_id` = `categories2_`.`product_id` WHERE `descriptio1_`.`language_id` = 1 AND `product0_`.`available` = 1 AND `product0_`.`date_available` <= '2019-10-21 21:17:32.7' AND `product0_`.`merchant_id` = 1 AND `product0_`.`manufacturer_id` = 1 AND `categories2_`.`category_id` IN (?)",
          "SELECT COUNT(DISTINCT `product0_`.`product_id`) AS `col_0_0_` FROM `product` AS `product0_` INNER JOIN `product_category` AS `categories2_` ON `product0_`.`product_id` = `categories2_`.`product_id` INNER JOIN `product_description` AS `descriptio1_` ON `categories2_`.`product_id` = `descriptio1_`.`product_id` WHERE `descriptio1_`.`product_highlight` = 1 AND `product0_`.`available` = 1 AND `product0_`.`date_available` <= '2019-10-21 21:17:32.7' AND `product0_`.`merchant_id` = 1 AND `product0_`.`manufacturer_id` = 1 AND `descriptio1_`.`product_id` IN (?)",
          "SELECT COUNT(DISTINCT `product0_`.`product_id`) AS `col_0_0_` FROM `product` AS `product0_` INNER JOIN `product_description` AS `descriptio1_` ON `product0_`.`product_id` = `descriptio1_`.`product_id` INNER JOIN `product_category` AS `categories2_` ON `descriptio1_`.`product_id` = `categories2_`.`product_id` WHERE `descriptio1_`.`language_id` = 1 AND `product0_`.`available` = 1 AND `product0_`.`date_available` <= '2019-10-21 21:17:32.7' AND `product0_`.`merchant_id` = 1 AND `product0_`.`manufacturer_id` = 1 AND `categories2_`.`category_id` IN (?)"
        };
    doTest(appName, stmtId, expected);
  }

  // 133 shopizer-68 slow

  //  @Test // 134
  void testShopizer119() {
    final String appName = "shopizer";
    final int stmtId = 119;
    final String[] expected =
        new String[] {
          "SELECT `productrel0_`.`product_relationship_id` AS `product_1_56_0_`, `product1_`.`product_id` AS `product_1_42_1_`, `product2_`.`product_id` AS `product_1_42_2_`, `descriptio3_`.`description_id` AS `descript1_46_3_`, `productrel0_`.`active` AS `active2_56_0_`, `productrel0_`.`code` AS `code3_56_0_`, `productrel0_`.`product_id` AS `product_4_56_0_`, `productrel0_`.`related_product_id` AS `related_5_56_0_`, `productrel0_`.`merchant_id` AS `merchant6_56_0_`, `product1_`.`date_created` AS `date_cre2_42_1_`, `product1_`.`date_modified` AS `date_mod3_42_1_`, `product1_`.`updt_id` AS `updt_id4_42_1_`, `product1_`.`available` AS `availabl5_42_1_`, `product1_`.`cond` AS `cond6_42_1_`, `product1_`.`date_available` AS `date_ava7_42_1_`, `product1_`.`manufacturer_id` AS `manufac25_42_1_`, `product1_`.`merchant_id` AS `merchan26_42_1_`, `product1_`.`customer_id` AS `custome27_42_1_`, `product1_`.`preorder` AS `preorder8_42_1_`, `product1_`.`product_height` AS `product_9_42_1_`, `product1_`.`product_free` AS `product10_42_1_`, `product1_`.`product_length` AS `product11_42_1_`, `product1_`.`quantity_ordered` AS `quantit12_42_1_`, `product1_`.`review_avg` AS `review_13_42_1_`, `product1_`.`review_count` AS `review_14_42_1_`, `product1_`.`product_ship` AS `product15_42_1_`, `product1_`.`product_virtual` AS `product16_42_1_`, `product1_`.`product_weight` AS `product17_42_1_`, `product1_`.`product_width` AS `product18_42_1_`, `product1_`.`ref_sku` AS `ref_sku19_42_1_`, `product1_`.`rental_duration` AS `rental_20_42_1_`, `product1_`.`rental_period` AS `rental_21_42_1_`, `product1_`.`rental_status` AS `rental_22_42_1_`, `product1_`.`sku` AS `sku23_42_1_`, `product1_`.`sort_order` AS `sort_or24_42_1_`, `product1_`.`tax_class_id` AS `tax_cla28_42_1_`, `product1_`.`product_type_id` AS `product29_42_1_`, `product2_`.`date_created` AS `date_cre2_42_2_`, `product2_`.`date_modified` AS `date_mod3_42_2_`, `product2_`.`updt_id` AS `updt_id4_42_2_`, `product2_`.`available` AS `availabl5_42_2_`, `product2_`.`cond` AS `cond6_42_2_`, `product2_`.`date_available` AS `date_ava7_42_2_`, `product2_`.`manufacturer_id` AS `manufac25_42_2_`, `product2_`.`merchant_id` AS `merchan26_42_2_`, `product2_`.`customer_id` AS `custome27_42_2_`, `product2_`.`preorder` AS `preorder8_42_2_`, `product2_`.`product_height` AS `product_9_42_2_`, `product2_`.`product_free` AS `product10_42_2_`, `product2_`.`product_length` AS `product11_42_2_`, `product2_`.`quantity_ordered` AS `quantit12_42_2_`, `product2_`.`review_avg` AS `review_13_42_2_`, `product2_`.`review_count` AS `review_14_42_2_`, `product2_`.`product_ship` AS `product15_42_2_`, `product2_`.`product_virtual` AS `product16_42_2_`, `product2_`.`product_weight` AS `product17_42_2_`, `product2_`.`product_width` AS `product18_42_2_`, `product2_`.`ref_sku` AS `ref_sku19_42_2_`, `product2_`.`rental_duration` AS `rental_20_42_2_`, `product2_`.`rental_period` AS `rental_21_42_2_`, `product2_`.`rental_status` AS `rental_22_42_2_`, `product2_`.`sku` AS `sku23_42_2_`, `product2_`.`sort_order` AS `sort_or24_42_2_`, `product2_`.`tax_class_id` AS `tax_cla28_42_2_`, `product2_`.`product_type_id` AS `product29_42_2_`, `descriptio3_`.`date_created` AS `date_cre2_46_3_`, `descriptio3_`.`date_modified` AS `date_mod3_46_3_`, `descriptio3_`.`updt_id` AS `updt_id4_46_3_`, `descriptio3_`.`description` AS `descript5_46_3_`, `descriptio3_`.`language_id` AS `languag14_46_3_`, `descriptio3_`.`name` AS `name6_46_3_`, `descriptio3_`.`title` AS `title7_46_3_`, `descriptio3_`.`meta_description` AS `meta_des8_46_3_`, `descriptio3_`.`meta_keywords` AS `meta_key9_46_3_`, `descriptio3_`.`meta_title` AS `meta_ti10_46_3_`, `descriptio3_`.`product_id` AS `product15_46_3_`, `descriptio3_`.`download_lnk` AS `downloa11_46_3_`, `descriptio3_`.`product_highlight` AS `product12_46_3_`, `descriptio3_`.`sef_url` AS `sef_url13_46_3_`, `descriptio3_`.`product_id` AS `product15_46_0__`, `descriptio3_`.`description_id` AS `descript1_46_0__` FROM `product_relationship` AS `productrel0_` INNER JOIN `product` AS `product2_` ON `productrel0_`.`related_product_id` = `product2_`.`product_id` INNER JOIN `product` AS `product1_` ON `productrel0_`.`product_id` = `product1_`.`product_id` INNER JOIN `product_description` AS `descriptio3_` ON `product2_`.`product_id` = `descriptio3_`.`product_id` WHERE `productrel0_`.`code` = 'RELATED_ITEM' AND `productrel0_`.`merchant_id` = 1 AND `product1_`.`product_id` = 2 AND `descriptio3_`.`language_id` = 1"
        };
    doTest(appName, stmtId, expected);
  }

  // 135 solidus-126 slow

  @Test // 136
  void testSolidus230() {
    final String appName = "solidus";
    final int stmtId = 230;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` WHERE `spree_taxonomies`.`name` LIKE '%style%' LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 137
  void testSolidus273() {
    final String appName = "solidus";
    final int stmtId = 273;
    final String[] expected =
        new String[] {
          "SELECT `spree_products_taxons`.`taxon_id` AS `taxon_id` FROM `spree_products_taxons` AS `spree_products_taxons` WHERE `spree_products_taxons`.`product_id` = 507",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 138
  void testSolidus277() {
    final String appName = "solidus";
    final int stmtId = 277;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 139
  void testSolidus279() {
    final String appName = "solidus";
    final int stmtId = 279;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 140
  void testSolidus312() {
    final String appName = "solidus";
    final int stmtId = 312;
    final String[] expected =
        new String[] {
          "SELECT `spree_option_values_variants`.`option_value_id` AS `option_value_id` FROM `spree_option_values_variants` AS `spree_option_values_variants` WHERE `spree_option_values_variants`.`variant_id` = 1477",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 141
  void testSolidus409() {
    final String appName = "solidus";
    final int stmtId = 409;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_payments` AS `spree_payments` WHERE `spree_payments`.`order_id` = 183 LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 142
  void testSolidus430() {
    final String appName = "solidus";
    final int stmtId = 430;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` WHERE `spree_stock_locations`.`active` = TRUE LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 143
  void testSolidus434() {
    final String appName = "solidus";
    final int stmtId = 434;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 144
  void testSolidus489() {
    final String appName = "solidus";
    final int stmtId = 489;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` WHERE `spree_product_properties`.`product_id` = 429 LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 145
  void testSolidus523() {
    final String appName = "solidus";
    final int stmtId = 523;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_promotion_codes` AS `spree_promotion_codes` WHERE `spree_promotion_codes`.`promotion_id` = 114 LIMIT 50 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 147
  void testSolidus649() {
    final String appName = "solidus";
    final int stmtId = 649;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` WHERE `spree_product_properties`.`product_id` = 426 LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 148
  void testSolidus652() {
    final String appName = "solidus";
    final int stmtId = 652;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_payments` AS `spree_payments` WHERE `spree_payments`.`order_id` = 180 LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 149
  void testSolidus656() {
    final String appName = "solidus";
    final int stmtId = 656;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` WHERE `spree_stock_locations`.`name` LIKE '%south%' LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 150
  void testSolidus658() {
    final String appName = "solidus";
    final int stmtId = 658;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 151
  void testSolidus681() {
    final String appName = "solidus";
    final int stmtId = 681;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_states` AS `spree_states` LIMIT 1 OFFSET 1) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 152
  void testSolidus744() {
    final String appName = "solidus";
    final int stmtId = 744;
    final String[] expected =
        new String[] {
          "SELECT COUNT(`spree_products`.`id`) FROM `spree_products` AS `spree_products` WHERE `spree_products`.`deleted_at` IS NULL OR `spree_products`.`deleted_at` >= '2020-05-16 05:07:38.510287'",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 153
  void testSolidus753() {
    final String appName = "solidus";
    final int stmtId = 753;
    final String[] expected =
        new String[] {
          "SELECT COUNT(`spree_products`.`id`) FROM `spree_products` AS `spree_products`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 154
  void testSolidus761() {
    final String appName = "solidus";
    final int stmtId = 761;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` WHERE `spree_zones`.`name` LIKE '%south%' LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 155
  void testSolidus765() {
    final String appName = "solidus";
    final int stmtId = 765;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 156
  void testSolidus767() {
    final String appName = "solidus";
    final int stmtId = 767;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 157
  void testSolidus794() {
    final String appName = "solidus";
    final int stmtId = 794;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_countries` AS `spree_countries` LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 158
  void testSolidus797() {
    final String appName = "solidus";
    final int stmtId = 797;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_countries` AS `spree_countries` LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 159
  void testSolidus799() {
    final String appName = "solidus";
    final int stmtId = 799;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_countries` AS `spree_countries` WHERE `spree_countries`.`name` LIKE '%zam%' LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 160
  void testSolidus818() {
    final String appName = "solidus";
    final int stmtId = 818;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `spree_roles_users` AS `spree_roles_users` WHERE `spree_roles_users`.`user_id` = 2401 AND `spree_roles_users`.`role_id` = 27",
          "SELECT COUNT(*) FROM `spree_roles_users` AS `spree_roles_users` WHERE `spree_roles_users`.`role_id` = 27 AND `spree_roles_users`.`user_id` = 2401"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 161
  void testSolidus835() {
    final String appName = "solidus";
    final int stmtId = 835;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` = 99 LIMIT 500 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 162
  void testSolidus837() {
    final String app = "solidus";
    final int stmtId = 837;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` = 78 LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 163
  void testSpree109() {
    final String app = "spree";
    final int stmtId = 109;
    final String[] expected = {
      "SELECT COUNT(`spree_orders`.`id`) FROM `spree_orders` AS `spree_orders` WHERE NOT `spree_orders`.`completed_at` IS NULL"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 164
  void testSpree125() {
    final String app = "spree";
    final int stmtId = 125;
    final String[] expected = {
      "SELECT COUNT(`spree_orders`.`id`) FROM `spree_orders` AS `spree_orders`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 165
  void testSpree285() {
    final String app = "spree";
    final int stmtId = 285;
    final String[] expected = {
      "SELECT `spree_adjustments`.`id` AS `id`, `spree_adjustments`.`source_type` AS `source_type`, `spree_adjustments`.`source_id` AS `source_id`, `spree_adjustments`.`adjustable_type` AS `adjustable_type`, `spree_adjustments`.`adjustable_id` AS `adjustable_id`, `spree_adjustments`.`amount` AS `amount`, `spree_adjustments`.`label` AS `label`, `spree_adjustments`.`mandatory` AS `mandatory`, `spree_adjustments`.`eligible` AS `eligible`, `spree_adjustments`.`created_at` AS `created_at`, `spree_adjustments`.`updated_at` AS `updated_at`, `spree_adjustments`.`state` AS `state`, `spree_adjustments`.`order_id` AS `order_id`, `spree_adjustments`.`included` AS `included` FROM `spree_adjustments` AS `spree_adjustments` INNER JOIN `spree_line_items` AS `spree_line_items` ON `spree_adjustments`.`adjustable_id` = `spree_line_items`.`id` WHERE `spree_line_items`.`order_id` = 1 AND `spree_adjustments`.`source_type` = 'Spree::TaxRate' AND `spree_adjustments`.`adjustable_type` = 'Spree::LineItem'"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 166
  void testSpree333() {
    final String app = "spree";
    final int stmtId = 333;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 3 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 167
  void testSpree393() {
    final String app = "spree";
    final int stmtId = 393;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT DISTINCT `spree_products`.`id` AS `id`, `spree_products`.`name` AS `name`, `spree_products`.`description` AS `description`, `spree_products`.`available_on` AS `available_on`, `spree_products`.`discontinue_on` AS `discontinue_on`, `spree_products`.`deleted_at` AS `deleted_at`, `spree_products`.`slug` AS `slug`, `spree_products`.`meta_description` AS `meta_description`, `spree_products`.`meta_keywords` AS `meta_keywords`, `spree_products`.`tax_category_id` AS `tax_category_id`, `spree_products`.`shipping_category_id` AS `shipping_category_id`, `spree_products`.`created_at` AS `created_at`, `spree_products`.`updated_at` AS `updated_at`, `spree_products`.`promotionable` AS `promotionable`, `spree_products`.`meta_title` AS `meta_title` FROM `spree_products` AS `spree_products` INNER JOIN `spree_variants` AS `spree_variants` ON `spree_products`.`id` = `spree_variants`.`product_id` INNER JOIN `spree_prices` AS `spree_prices` ON `spree_variants`.`id` = `spree_prices`.`variant_id` WHERE `spree_variants`.`is_master` = TRUE AND `spree_variants`.`deleted_at` IS NULL AND (`spree_products`.`discontinue_on` IS NULL OR `spree_products`.`discontinue_on` >= '2020-05-01 07:05:53.714089') AND `spree_products`.`deleted_at` IS NULL AND (`spree_products`.`deleted_at` IS NULL OR `spree_products`.`deleted_at` >= '2020-05-01 07:05:53.713734') AND `spree_products`.`available_on` <= '2020-05-01 07:05:53.714072' AND `spree_prices`.`deleted_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 168
  void testSpree396() {
    final String app = "spree";
    final int stmtId = 396;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT DISTINCT `spree_products`.`id` AS `id`, `spree_products`.`name` AS `name`, `spree_products`.`description` AS `description`, `spree_products`.`available_on` AS `available_on`, `spree_products`.`discontinue_on` AS `discontinue_on`, `spree_products`.`deleted_at` AS `deleted_at`, `spree_products`.`slug` AS `slug`, `spree_products`.`meta_description` AS `meta_description`, `spree_products`.`meta_keywords` AS `meta_keywords`, `spree_products`.`tax_category_id` AS `tax_category_id`, `spree_products`.`shipping_category_id` AS `shipping_category_id`, `spree_products`.`created_at` AS `created_at`, `spree_products`.`updated_at` AS `updated_at`, `spree_products`.`promotionable` AS `promotionable`, `spree_products`.`meta_title` AS `meta_title` FROM `spree_products` AS `spree_products` INNER JOIN `spree_variants` AS `spree_variants` ON `spree_products`.`id` = `spree_variants`.`product_id` INNER JOIN `spree_prices` AS `spree_prices` ON `spree_variants`.`id` = `spree_prices`.`variant_id` WHERE `spree_variants`.`is_master` = TRUE AND `spree_variants`.`deleted_at` IS NULL AND (`spree_products`.`discontinue_on` IS NULL OR `spree_products`.`discontinue_on` >= '2020-05-01 07:05:55.179826') AND `spree_products`.`deleted_at` IS NULL AND (`spree_products`.`deleted_at` IS NULL OR `spree_products`.`deleted_at` >= '2020-05-01 07:05:55.179498') AND `spree_products`.`available_on` <= '2020-05-01 07:05:55.179810' AND `spree_prices`.`deleted_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 169
  void testSpree402() {
    final String app = "spree";
    final int stmtId = 402;
    final String[] expected = {
      "SELECT COUNT(`spree_products`.`id`) FROM `spree_products` AS `spree_products`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 170
  void testSpree414() {
    final String app = "spree";
    final int stmtId = 414;
    final String[] expected = {
      "SELECT COUNT(`spree_products`.`id`) FROM `spree_products` AS `spree_products` WHERE `spree_products`.`deleted_at` IS NULL AND (`spree_products`.`deleted_at` IS NULL OR `spree_products`.`deleted_at` >= '2020-05-01 07:06:03.678713')"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 171
  void testSpree447() {
    final String app = "spree";
    final int stmtId = 447;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 172
  void testSpree524() {
    final String app = "spree";
    final int stmtId = 524;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 658 AND NOT `spree_orders`.`completed_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 173
  void testSpree528() {
    final String app = "spree";
    final int stmtId = 528;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 660 AND NOT `spree_orders`.`completed_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 174
  void testSpree585() {
    final String app = "spree";
    final int stmtId = 585;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` = 6 LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 175
  void testSpree587() {
    final String app = "spree";
    final int stmtId = 587;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` = 17 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 176
  void testSpree589() {
    final String app = "spree";
    final int stmtId = 589;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`name` LIKE '%Imaginary%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 177
  void testSpree591() {
    final String app = "spree";
    final int stmtId = 591;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 178
  void testSpree614() {
    final String app = "spree";
    final int stmtId = 614;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_payments` AS `spree_payments` WHERE `spree_payments`.`order_id` = 23 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 179
  void testSpree620() {
    final String app = "spree";
    final int stmtId = 620;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_payments` AS `spree_payments` WHERE `spree_payments`.`order_id` = 45 LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 180
  void testSpree632() {
    final String app = "spree";
    final int stmtId = 632;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 275 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 181
  void testSpree634() {
    final String app = "spree";
    final int stmtId = 634;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 277 AND NOT `spree_orders`.`completed_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 182
  void testSpree657() {
    final String app = "spree";
    final int stmtId = 657;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` WHERE `spree_stock_locations`.`name` LIKE '%south%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 183
  void testSpree659() {
    final String app = "spree";
    final int stmtId = 659;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 184
  void testSpree662() {
    final String app = "spree";
    final int stmtId = 662;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 185
  void testSpree691() {
    final String app = "spree";
    final int stmtId = 691;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 186
  void testSpree693() {
    final String app = "spree";
    final int stmtId = 693;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 187
  void testSpree695() {
    final String app = "spree";
    final int stmtId = 695;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` WHERE `spree_zones`.`name` LIKE '%south%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 188
  void testSpree712() {
    final String app = "spree";
    final int stmtId = 712;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT DISTINCT `spree_products`.`id` AS `id`, `spree_products`.`name` AS `name`, `spree_products`.`description` AS `description`, `spree_products`.`available_on` AS `available_on`, `spree_products`.`discontinue_on` AS `discontinue_on`, `spree_products`.`deleted_at` AS `deleted_at`, `spree_products`.`slug` AS `slug`, `spree_products`.`meta_description` AS `meta_description`, `spree_products`.`meta_keywords` AS `meta_keywords`, `spree_products`.`tax_category_id` AS `tax_category_id`, `spree_products`.`shipping_category_id` AS `shipping_category_id`, `spree_products`.`created_at` AS `created_at`, `spree_products`.`updated_at` AS `updated_at`, `spree_products`.`promotionable` AS `promotionable`, `spree_products`.`meta_title` AS `meta_title` FROM `spree_products` AS `spree_products` INNER JOIN `spree_variants` AS `spree_variants` ON `spree_products`.`id` = `spree_variants`.`product_id` INNER JOIN `spree_prices` AS `spree_prices` ON `spree_variants`.`id` = `spree_prices`.`variant_id` WHERE `spree_variants`.`is_master` = TRUE AND `spree_variants`.`deleted_at` IS NULL AND (`spree_products`.`discontinue_on` IS NULL OR `spree_products`.`discontinue_on` >= '2020-05-01 07:07:42.906418') AND `spree_products`.`deleted_at` IS NULL AND `spree_products`.`available_on` <= '2020-05-01 07:07:42.906389' AND `spree_prices`.`deleted_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 189
  void testSpree726() {
    final String app = "spree";
    final int stmtId = 726;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` WHERE `spree_product_properties`.`product_id` = 1059 LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 190
  void testSpree729() {
    final String app = "spree";
    final int stmtId = 729;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` WHERE `spree_product_properties`.`product_id` = 1061 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 191
  void testSpree731() {
    final String app = "spree";
    final int stmtId = 731;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` INNER JOIN `spree_properties` AS `spree_properties` ON `spree_product_properties`.`property_id` = `spree_properties`.`id` WHERE `spree_properties`.`name` LIKE '%size%' AND `spree_product_properties`.`product_id` = 1063 LIMIT 25 OFFSET 0) AS `subquery_for_count`",
    };
    doTest(app, stmtId, expected);
  }

  @Test // 192
  void testSpree766() {
    final String app = "spree";
    final int stmtId = 766;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_reimbursements` AS `spree_reimbursements` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 193
  void testSpree781() {
    final String app = "spree";
    final int stmtId = 781;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_customer_returns` AS `spree_customer_returns` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 194
  void testSpree783() {
    final String app = "spree";
    final int stmtId = 783;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` WHERE `spree_taxonomies`.`name` LIKE '%style%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 195
  void testSpree785() {
    final String app = "spree";
    final int stmtId = 785;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 196
  void testSpree787() {
    final String app = "spree";
    final int stmtId = 787;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 197
  void testSpree831() {
    final String app = "spree";
    final int stmtId = 831;
    final String[] expected = {
      "SELECT `spree_promotion_rule_taxons`.`taxon_id` AS `taxon_id` FROM `spree_promotion_rule_taxons` AS `spree_promotion_rule_taxons` WHERE `spree_promotion_rule_taxons`.`promotion_rule_id` = 44"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 198
  void testSpree1147() {
    final String app = "spree";
    final int stmtId = 1147;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_transfers` AS `spree_stock_transfers` WHERE `spree_stock_transfers`.`destination_location_id` = 4 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 199
  void testSpree1150() {
    final String app = "spree";
    final int stmtId = 1150;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_transfers` AS `spree_stock_transfers` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 200
  void testSpree1151() {
    final String app = "spree";
    final int stmtId = 1151;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_transfers` AS `spree_stock_transfers` WHERE `spree_stock_transfers`.`source_location_id` = 1 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }
}
