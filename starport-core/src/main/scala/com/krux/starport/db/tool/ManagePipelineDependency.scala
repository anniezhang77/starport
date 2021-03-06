package com.krux.starport.db.tool

import scopt.OptionParser
import slick.jdbc.PostgresProfile.api._

import com.krux.starport.config.StarportSettings
import com.krux.starport.db.record.PipelineDependency
import com.krux.starport.db.table.PipelineDependencies
import com.krux.starport.db.WaitForIt
import com.krux.starport.{BuildInfo, Logging}


object ManagePipelineDependency extends WaitForIt with Logging {

  lazy val starportSettings = StarportSettings()

  case class Options(
    add: Boolean = false,
    remove: Boolean = false,
    pipelineId: Int = 0,
    upstreamPipelineId: Int = 0
  )

  def main(args: Array[String]): Unit = {

    val parser = new OptionParser[Options]("manage-pipeline-dependency") {

      head("manage-pipeline-dependency", s"${BuildInfo.version}")
      help("help") text "prints this usage text"

      opt[Unit]('a', "add").action((_, c) => c.copy(add = true))
        .text("add a pipeline dependency")
        .optional()

      opt[Unit]('r', "remove").action((_, c) => c.copy(remove = true))
        .text("remove a pipeline dependency")
        .optional()

      opt[Int]("pid").action((x, c) => c.copy(pipelineId = x))
        .text("pipeline id")
        .required()

      opt[Int]("upstream-pid").action((x, c) => c.copy(upstreamPipelineId = x))
        .text("upstream pipeline id")
        .required()
    }

    parser.parse(args, Options()).foreach { cli =>

      val db = starportSettings.jdbc.db

      if (cli.add) {
        val pipelineDependencyRecord = PipelineDependency(cli.pipelineId, cli.upstreamPipelineId)
        db.run(DBIO.seq(PipelineDependencies() += pipelineDependencyRecord)).waitForResult
        logger.info(s"Added pipeline dependency ${cli.upstreamPipelineId} => ${cli.pipelineId}")
      } else if (cli.remove) {
        db.run(
          PipelineDependencies()
            .filter(pd => pd.pipelineId === cli.pipelineId && pd.upstreamPipelineId === cli.upstreamPipelineId)
            .delete
        ).waitForResult
        logger.info(s"Removed pipeline dependency ${cli.upstreamPipelineId} => ${cli.pipelineId}")
      } else {
        logger.info("No action needed.")
      }

      logger.info("Done")
    }
  }
}
